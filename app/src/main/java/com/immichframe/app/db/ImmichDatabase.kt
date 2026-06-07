package com.immichframe.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlbumEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ImmichDatabase : RoomDatabase() {
    abstract fun immichDao(): ImmichDao

    companion object {
        @Volatile private var instance: ImmichDatabase? = null

        fun get(context: Context): ImmichDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ImmichDatabase::class.java,
                "immich.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
