package com.ptsl.fwa_networksdk_hostapp.model

data class RecentTest(
    val timestamp: String,
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val rsrp: Int,
    val status: String
)

