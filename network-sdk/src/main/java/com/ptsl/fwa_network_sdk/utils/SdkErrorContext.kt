package com.ptsl.fwa_network_sdk.utils

import androidx.annotation.Keep

/**
 * Immutable value object representing the resolved context for an SDK exception.
 *
 * @property errorCode    Internal machine-readable error code (see [Constants] ERR_CODE_* values).
 * @property userMessage  Human-readable message surfaced to the host-app callback.
 * @property statusCode   HTTP-style status code included in [com.ptsl.fwa_network_sdk.data_model.NetworkDataResponse].
 */
@Keep
internal data class SdkErrorContext(
    val errorCode: String,
    val userMessage: String,
    val statusCode: Int
)
