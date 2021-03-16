package com.tommihirvonen.exifnotes.datastructures

enum class PartialIncrement {
    THIRD, HALF;
    companion object {
        fun from(value: Int) = values().firstOrNull { it.ordinal == value } ?: THIRD
    }
}