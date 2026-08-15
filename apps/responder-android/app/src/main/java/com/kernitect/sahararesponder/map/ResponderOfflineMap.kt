package com.kernitect.sahararesponder.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.database.sqlite.SQLiteDatabase
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import java.io.File
import kotlin.math.*

object OfflineMapConfig {
    const val DIRECTORY = "offline_maps"
    const val FILE_NAME = "chitwan_offline.mbtiles"
    const val ASSET_PATH = "$DIRECTORY/$FILE_NAME"
    const val MIN_ZOOM = 10.0
    const val MAX_ZOOM = 17.0
    fun archive(context: Context) = File(File(context.filesDir, DIRECTORY), FILE_NAME)
}

enum class ResponderMapMode { ONLINE, OFFLINE, OFFLINE_PACK_MISSING, OFFLINE_COVERAGE_UNAVAILABLE }

fun selectMapMode(networkAvailable: Boolean, archiveAvailable: Boolean, coverageAvailable: Boolean = true) = when {
    networkAvailable -> ResponderMapMode.ONLINE
    !archiveAvailable -> ResponderMapMode.OFFLINE_PACK_MISSING
    coverageAvailable -> ResponderMapMode.OFFLINE
    else -> ResponderMapMode.OFFLINE_COVERAGE_UNAVAILABLE
}

fun activeMapIncidents(incidents: List<com.kernitect.sahararesponder.model.ResponderIncident>) =
    incidents.filter { it.status != "RESCUED" }

class ResponderOfflineMapManager(private val context: Context) {
    fun prepareArchive(): File? {
        val target = OfflineMapConfig.archive(context)
        if (target.isFile && target.length() > 0 && validate(target)) return target.also { Log.i(TAG, "Offline map archive found") }
        return runCatching {
            target.parentFile?.mkdirs()
            context.assets.open(OfflineMapConfig.ASSET_PATH).use { input -> target.outputStream().use(input::copyTo) }
            target.takeIf { it.length() > 0 && validate(it) }
        }.onSuccess { if (it != null) Log.i(TAG, "Offline archive copied") }
            .onFailure { Log.i(TAG, "Offline map archive missing") }.getOrNull()
    }

    private fun validate(file: File): Boolean = runCatching {
        val archive = ArchiveFileFactory.getArchiveFile(file) ?: return false
        archive.close(); true
    }.onFailure { Log.w(TAG, "Offline tile provider initialization failed", it) }.getOrDefault(false)

    fun hasNetwork(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun offlineProvider(archive: File) = OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(archive))

    fun hasCoverage(archive: File, points: List<Pair<Double, Double>>, zoom: Int = 12): Boolean = runCatching {
        val checkPoints = points.ifEmpty { listOf(27.68 to 84.43) }
        SQLiteDatabase.openDatabase(archive.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            checkPoints.any { (latitude, longitude) ->
                val n = 1 shl zoom
                val x = floor((longitude + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
                val latRadians = Math.toRadians(latitude.coerceIn(-85.0511, 85.0511))
                val xyzY = floor((1.0 - ln(tan(latRadians) + 1.0 / cos(latRadians)) / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
                val tileRow = n - 1 - xyzY
                database.rawQuery(
                    "SELECT 1 FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1",
                    arrayOf(zoom.toString(), x.toString(), tileRow.toString()),
                ).use { it.moveToFirst() }
            }
        }
    }.onFailure { Log.w(TAG, "Offline coverage lookup failed", it) }.getOrDefault(false)

    companion object { private const val TAG = "SaharaResponder" }
}
