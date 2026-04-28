package com.ptsl.fwa_network_sdk.data_model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.ptsl.fwa_network_sdk.BuildConfig
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity()
@Keep
data class AuthEntity(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("apiKey") var apiKey: String = BuildConfig.TOKEN,
    @SerializedName("userType") var userType: Int = 0,
    @SerializedName("sdkVersion") var sdkVersion: String = "",

    @SerializedName("isSdkInitialized") var isSdkInitialized: Boolean = false,
    @SerializedName("isLocationEnabled") var isLocationEnabled: Boolean = false,
    @SerializedName("isPhoneStateEnabled") var isPhoneStateEnabled: Boolean = false,
    @SerializedName("hostAppName") var hostAppName: String = "",
) : Parcelable
