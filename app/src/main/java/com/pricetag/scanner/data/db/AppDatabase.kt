package com.pricetag.scanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pricetag.scanner.data.db.dao.JobDao
import com.pricetag.scanner.data.db.entity.JobEntity

@Database(
    entities  = [JobEntity::class],
    version   = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
}
