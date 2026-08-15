package com.kernitect.saharaandroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kernitect.saharaandroid.data.local.dao.IncidentDao
import com.kernitect.saharaandroid.data.local.dao.PublicAlertDao
import com.kernitect.saharaandroid.data.local.entity.IncidentEntity
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity

@Database(
    entities = [
        IncidentEntity::class,
        PublicAlertEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SaharaDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    abstract fun publicAlertDao(): PublicAlertDao

    companion object {

        @Volatile
        private var INSTANCE: SaharaDatabase? = null

        /*
         * Database version 1 → 2
         *
         * Adds locally stored public emergency alerts.
         */
        private val MIGRATION_1_2 =
            object : Migration(
                1,
                2
            ) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `public_alerts` (
                            `id` TEXT NOT NULL,
                            `province` TEXT NOT NULL,
                            `district` TEXT NOT NULL,
                            `municipality` TEXT NOT NULL,
                            `disasterType` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `latitude` REAL NOT NULL,
                            `longitude` REAL NOT NULL,
                            `affectedRadiusMeters` REAL NOT NULL,
                            `severity` TEXT NOT NULL,
                            `message` TEXT NOT NULL,
                            `startsAt` INTEGER NOT NULL,
                            `expiresAt` INTEGER NOT NULL,
                            `isDemo` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )

                    seedPublicAlerts(
                        db
                    )
                }
            }


        /*
         * Seed alerts when the database
         * is created for the first time.
         */
        private val DATABASE_CALLBACK =
            object : RoomDatabase.Callback() {

                override fun onCreate(
                    db: SupportSQLiteDatabase
                ) {

                    super.onCreate(
                        db
                    )

                    seedPublicAlerts(
                        db
                    )
                }
            }


        fun getInstance(
            context: Context
        ): SaharaDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            SaharaDatabase::class.java,
                            "sahara_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2
                            )
                            .addCallback(
                                DATABASE_CALLBACK
                            )
                            .build()

                    INSTANCE =
                        instance

                    instance
                }
        }


        /*
         * ============================================
         * BAGMATI PROVINCE PUBLIC ALERT SAMPLE DATA
         * ============================================
         *
         * These records are stored locally so the
         * emergency-warning system can be demonstrated
         * completely offline.
         *
         * isDemo remains internal metadata only.
         */
        private fun seedPublicAlerts(
            db: SupportSQLiteDatabase
        ) {

            /*
             * Long expiry for prototype testing.
             */
            val alertExpiry =
                4102444800000L


            /*
             * ========================================
             * EARTHQUAKE
             * ========================================
             */
            db.execSQL(
                """
                INSERT OR IGNORE INTO public_alerts (
                    id,
                    province,
                    district,
                    municipality,
                    disasterType,
                    title,
                    latitude,
                    longitude,
                    affectedRadiusMeters,
                    severity,
                    message,
                    startsAt,
                    expiresAt,
                    isDemo
                )
                VALUES (
                    'bagmati-earthquake-001',
                    'Bagmati Province',
                    'Chitwan',
                    'Bharatpur',
                    'EARTHQUAKE',
                    'Chitwan Earthquake Alert',
                    27.6766,
                    84.4350,
                    30000.0,
                    'VERY_HIGH',
                    'Earthquake alert active in your area. Move to an open space and stay away from damaged structures.',
                    0,
                    $alertExpiry,
                    1
                )
                """.trimIndent()
            )


            /*
             * ========================================
             * FLOOD
             * ========================================
             */
            db.execSQL(
                """
                INSERT OR IGNORE INTO public_alerts (
                    id,
                    province,
                    district,
                    municipality,
                    disasterType,
                    title,
                    latitude,
                    longitude,
                    affectedRadiusMeters,
                    severity,
                    message,
                    startsAt,
                    expiresAt,
                    isDemo
                )
                VALUES (
                    'bagmati-flood-001',
                    'Bagmati Province',
                    'Chitwan',
                    'Bharatpur',
                    'FLOOD',
                    'Bharatpur Flood Alert',
                    27.6833,
                    84.4333,
                    15000.0,
                    'HIGH',
                    'Flood alert active in your area. Avoid low-lying areas and move toward safer higher ground.',
                    0,
                    $alertExpiry,
                    1
                )
                """.trimIndent()
            )


            /*
             * ========================================
             * LANDSLIDE
             * ========================================
             */
            db.execSQL(
                """
                INSERT OR IGNORE INTO public_alerts (
                    id,
                    province,
                    district,
                    municipality,
                    disasterType,
                    title,
                    latitude,
                    longitude,
                    affectedRadiusMeters,
                    severity,
                    message,
                    startsAt,
                    expiresAt,
                    isDemo
                )
                VALUES (
                    'bagmati-landslide-001',
                    'Bagmati Province',
                    'Chitwan',
                    'Ichchhakamana',
                    'LANDSLIDE',
                    'Ichchhakamana Landslide Alert',
                    27.8560,
                    84.5570,
                    10000.0,
                    'HIGH',
                    'Landslide alert active in your area. Avoid unstable slopes and potentially blocked road sections.',
                    0,
                    $alertExpiry,
                    1
                )
                """.trimIndent()
            )
        }
    }
}