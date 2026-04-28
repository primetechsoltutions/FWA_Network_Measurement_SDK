package com.ptsl.fwa_network_sdk.data_model.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import androidx.annotation.Keep


@Parcelize
@Keep
class FTPCellInfoGetRequest
    (
    @SerializedName("enodeBId")
    var eNB: Int = 0,

    @SerializedName("sector")
    var cID: Int = 0,
): Parcelable

