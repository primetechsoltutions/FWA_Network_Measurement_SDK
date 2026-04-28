package com.ptsl.fwa_network_sdk.data_model.logger

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class LogDataWrapper(
    @SerializedName("networkUserModel") val auth: AuthEntity,
    @SerializedName("unifiedNetworkLogs") val eventLog: List<EventLogModel>
) : Parcelable
