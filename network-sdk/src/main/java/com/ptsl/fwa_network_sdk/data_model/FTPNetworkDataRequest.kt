package com.ptsl.fwa_network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity

@Keep
data class FTPNetworkDataRequest (
    @SerializedName("networkUserModel" ) var auth : AuthEntity,
    @SerializedName("assessmentData" ) var data : FTPNetworkDataEntity,
)
