package com.ptsl.fwa_network_sdk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.data_model.logger.EventLogModel


@Dao
interface NetworkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthData(authRequest: AuthEntity): Long

    @Query("SELECT * FROM authentity")
    suspend fun getPersistentAuth(): AuthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkDataLogIntoDB(logModel: EventLogModel): Long

    @Query("SELECT * FROM eventlogmodel")
    suspend fun getNetworkDataLogEvent(): List<EventLogModel>

    @Query("DELETE FROM eventlogmodel")
    suspend fun deleteNetworkDataLogEvent()

    @Query("SELECT COUNT(*) FROM eventlogmodel")
    suspend fun getNetworkDataLogEventCount(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFTPThresholds(thresholds: FTPThresholdEntity)

    @Query("SELECT * FROM ftpthresholdentity LIMIT 1")
    suspend fun getFTPThresholds(): FTPThresholdEntity?
}