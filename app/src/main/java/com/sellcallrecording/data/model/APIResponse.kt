package com.sellcallrecording.data.model

import java.io.Serializable


data class APIResponse(
    val status: String,
    val msg: String,
    val token: String,
    val type: String?,
    val ctype: List<CallType>?
) : Serializable
