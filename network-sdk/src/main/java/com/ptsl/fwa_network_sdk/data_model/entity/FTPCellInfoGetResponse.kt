package com.ptsl.fwa_network_sdk.data_model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class FTPCellInfoGetResponse(

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @SerializedName("ENodeBName")
    var eNodeBName: String = "",

    @SerializedName("CellName")
    var cellName: String = "",

    @SerializedName("ENodeBId")
    var eNodeBId: Long = 0L,

    @SerializedName("Sector")
    var sector: Int = 0,

    @SerializedName("Band")
    var band: String = "",

    @SerializedName("NbhDlThroughputMbps")
    var nbhDlThroughputMbps: Double = 0.0,

    @SerializedName("NbhTrafficGb")
    var nbhTrafficGb: Double = 0.0

) : Parcelable