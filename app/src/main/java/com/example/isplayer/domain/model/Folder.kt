package com.example.isplayer.domain.model

data class Folder(
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false
)
