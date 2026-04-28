package com.ptsl.networksdk_event.model

data class RecentTest(
    val timestamp: String,
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val rsrp: Int,
    val status: String
)
