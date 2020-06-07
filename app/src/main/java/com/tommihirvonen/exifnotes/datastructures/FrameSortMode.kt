package com.tommihirvonen.exifnotes.datastructures

enum class FrameSortMode constructor(value: Int) {
    FRAME_COUNT(0),
    DATE(1),
    F_STOP(2),
    SHUTTER_SPEED(3),
    LENS(4);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    companion object {

        fun fromValue(value: Int): FrameSortMode {
            return when (value) {
                0 -> FRAME_COUNT
                1 -> DATE
                2 -> F_STOP
                3 -> SHUTTER_SPEED
                4 -> LENS
                else -> FRAME_COUNT
            }
        }
    }
}
