package com.ptsl.fwa_network_sdk.data_model

data class NetworkDataResponse (
    val status: String? = null,
    val testResult: String? = null,
    val statusCode: Int? = null,
    val message: String? = null,
    val data: AssessmentResult? = null
)