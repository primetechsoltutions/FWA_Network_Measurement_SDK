package com.ptsl.fwa_network_sdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.data_model.logger.EventLogModel

@Database(
    entities = [AuthEntity::class, EventLogModel::class, FTPThresholdEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NetworkDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
}