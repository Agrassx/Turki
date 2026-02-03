package com.turki.core.domain

import kotlinx.datetime.Instant

data class UserState(
    val userId: Long,
    val state: String,
    val payload: String,
    val updatedAt: Instant
)
