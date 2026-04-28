package com.ptsl.fwa_network_sdk.data_model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class AssessmentDataResponseEntity(

    @SerializedName("assessmentId")
    var assessmentId: Long = 0L

) : Parcelable