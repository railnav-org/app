package com.app.railnav.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// BUMP VERSION TO 4 for on-demand streaming schema changes
@Database(
    entities = [NodeEntity::class, EdgeEntity::class, StationHubEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RailNavDatabase : RoomDatabase() {

    abstract fun graphDao(): GraphDao

    companion object {
        @Volatile
        private var INSTANCE: RailNavDatabase? = null

        fun getDatabase(context: Context): RailNavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RailNavDatabase::class.java,
                    "railnav_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * No-op: Asset seeding has been replaced by on-demand network streaming.
         * Station data is now fetched from Supabase PostGIS when the user
         * approaches a transit hub (geofence trigger).
         *
         * This method is kept as a stub for backwards compatibility with any
         * existing callers, but it does nothing.
         */
        @Suppress("UNUSED_PARAMETER")
        suspend fun seedDatabaseIfEmpty(context: Context, database: RailNavDatabase) {
            // Intentionally empty — data is now streamed on-demand from Supabase
        }
    }
}