package com.ptsl.fwa_network_sdk.data_model


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BaseResponse<T>(
    @SerializedName("Data" ) var data : T?,
    @SerializedName("Status" ) var statusCode : Int,
    @SerializedName("Message" ) var message : String,
)