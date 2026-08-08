package com.lastgimbus.the.freebuddy.protocol

/** Huawei reports 0 for an unavailable component and 1..100 for a real level. */
object BatteryLevel {
    const val SEGMENT_COUNT = 20

    fun fromProtocol(value: Byte): Int? =
        (value.toInt() and 0xFF).takeIf { it in 1..100 }

    fun validOrNull(value: Int?): Int? = value?.takeIf { it in 1..100 }

    fun fraction(value: Int?): Float = (validOrNull(value) ?: 0) / 100f

    /** Number of filled 5% slots used by size-independent Glance widgets. */
    fun segments(value: Int?): Int =
        validOrNull(value)?.let { (it + 2) / 5 }?.coerceIn(0, SEGMENT_COUNT) ?: 0
}
