package com.ptsl.fwa_network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPCellInfoGetRequest

@Keep
data class FTPCellInfoGetDataRequest (
    @SerializedName("networkUserModel" ) var auth : AuthEntity,
    @SerializedName("cellInfoByNodeSectorModel" ) var data : FTPCellInfoGetRequest,
)
