package com.footballxtream.model

/** A single EPG programme for a channel (times are epoch millis, 0 when unknown). */
data class EpgProgram(
    val title: String,
    val start: Long,
    val end: Long,
    val nowFlag: Boolean,
)
