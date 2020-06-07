package com.tommihirvonen.exifnotes.datastructures

enum class RollSortMode constructor(value: Int) {
    DATE(0),
    NAME(1),
    CAMERA(2);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    companion object {

        fun fromValue(value: Int): RollSortMode {
            return when (value) {
                0 -> DATE
                1 -> NAME
                2 -> CAMERA
                else -> DATE
            }
        }
    }
}
