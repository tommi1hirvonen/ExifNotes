package com.tommihirvonen.exifnotes.datastructures

/**
 * Enum class to describe roll filter modes.
 */
enum class FilterMode(value: Int) {
    ACTIVE(0),
    ARCHIVED(1),
    ALL(2);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    companion object {

        fun fromValue(value: Int): FilterMode {
            return when (value) {
                0 -> ACTIVE
                1 -> ARCHIVED
                2 -> ALL
                else -> ACTIVE
            }
        }
    }
}
