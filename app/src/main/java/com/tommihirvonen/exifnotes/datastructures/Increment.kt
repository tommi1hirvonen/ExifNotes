package com.tommihirvonen.exifnotes.datastructures

enum class Increment {
    THIRD, HALF, FULL;
    companion object {
        fun from(value: Int) = values().firstOrNull { it.ordinal == value } ?: THIRD
    }
}