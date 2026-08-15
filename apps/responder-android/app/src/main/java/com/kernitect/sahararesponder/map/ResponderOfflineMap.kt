package com.kernitect.sahararesponder.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import java.io.File

object OfflineMapConfig {
    const val DIRECTORY = "offline_maps"
    const val FILE_NAME = "chitwan_offline.mbtiles"
    const val ASSET_PATH = "$DIRECTORY/$FILE_NAME"
    const val MIN_ZOOM = 10.0
    const val MAX_ZOOM = 17.0
    fun archive(context: Context) = File(File(context.filesDir, DIRECTORY), FILE_NAME)
}

enum class ResponderMapMode { ONLINE, OFFLINE, OFFLINE_COVERAGE_UNAVAILABLE }

fun selectMapMode(networkAvailable: Boolean, archiveAvailable: Boolean) = when {
    networkAvailable -> ResponderMapMode.ONLINE
    archiveAvailable -> ResponderMapMode.OFFLINE
    else -> ResponderMapMode.OFFLINE_COVERAGE_UNAVAILABLE
}

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

    companion object { private const val TAG = "SaharaResponder" }
}
