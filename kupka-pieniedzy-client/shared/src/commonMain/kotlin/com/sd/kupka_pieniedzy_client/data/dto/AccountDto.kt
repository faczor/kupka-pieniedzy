package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wiersz `accounts`. */
@Serializable
data class AccountDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: String = "checking",
    @SerialName("currency") val currency: String = "PLN",
)
