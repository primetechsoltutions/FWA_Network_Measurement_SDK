package com.ptsl.fwa_network_sdk.data_model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class FTPNetworkDataEntity(

    var id: Long = 0L,

    @SerializedName("integratedAppEventName")
    var integratedAppEventName: String = "",

    @SerializedName("integratedAppVersion")
    var integratedAppVersion: String = "",

    @SerializedName("cid")
    var cid: Int = 0,

    @SerializedName("latitude")
    var latitude: Double = 0.0,

    @SerializedName("longitude")
    var longitude: Double = 0.0,

    @SerializedName("userLatitude")
    var userLatitude: Double = 0.0,

    @SerializedName("userLongitude")
    var userLongitude: Double = 0.0,

    @SerializedName("ulspeed")
    var ulSpeed: Double = 0.0,

    @SerializedName("dlspeed")
    var dlSpeed: Double = 0.0,

    @SerializedName("date")
    var date: String = "",

    @SerializedName("deviceManufacture")
    var deviceManufacture: String = "",

    @SerializedName("deviceModel")
    var deviceModel: String = "",

    @SerializedName("deviceOsVersion")
    var deviceOsVersion: String = "",

    @SerializedName("band")
    var band: String = "",

    @SerializedName("internetConnectivityType")
    var internetConnectivityType: String = "",

    @SerializedName("mcc")
    var mcc: String = "",

    @SerializedName("mnc")
    var mnc: String = "",

    @SerializedName("msisdn")
    var msisdn: String = "",

    @SerializedName("sdkInitiateTimeStamp")
    var sdkInitiateTimeStamp: String = "",

    @SerializedName("totalDownloadVolume")
    var totalDownloadVolume: Double = 0.0,

    @SerializedName("totalUploadVolume")
    var totalUploadVolume: Double = 0.0,

    @SerializedName("technologyType")
    var technologyType: String = "",

    @SerializedName("rsrp")
    var rsrp: Int = 0,

    @SerializedName("enb")
    var enb: Int = 0,

    @SerializedName("tac")
    var tac: Int = 0,

    @SerializedName("rsrq")
    var rsrq: Int = 0,

    @SerializedName("snr")
    var snr: Int = 0,

    @SerializedName("eNodeBName")
    var eNodeBName: String = "",

    @SerializedName("cellName")
    var cellName: String = "",

    @SerializedName("eNodeBId")
    var eNodeBId: Long = 0L,

    @SerializedName("sector")
    var sector: Int = 0,

    @SerializedName("nbhDlThroughputMbps")
    var nbhDlThroughputMbps: Double = 0.0,

    @SerializedName("nbhTrafficGb")
    var nbhTrafficGb: Double = 0.0

) : Parcelable