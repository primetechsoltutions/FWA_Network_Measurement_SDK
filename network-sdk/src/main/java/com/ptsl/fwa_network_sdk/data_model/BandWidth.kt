package com.ptsl.fwa_network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BandWidth(
    @SerializedName("UploadFile" ) var uploadFile : String ="",
    @SerializedName("Type" ) var type : String    =""
)
