package com.tommihirvonen.exifnotes.datastructures

enum class RollFilterMode(value: Int) {
    ACTIVE(0),
    ARCHIVED(1),
    ALL(2);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    companion object {

        fun fromValue(value: Int): RollFilterMode {
            return when (value) {
                0 -> ACTIVE
                1 -> ARCHIVED
                2 -> ALL
                else -> ACTIVE
            }
        }
    }
}
