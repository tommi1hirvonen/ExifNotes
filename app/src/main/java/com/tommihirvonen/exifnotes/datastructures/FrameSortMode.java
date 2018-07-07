package com.tommihirvonen.exifnotes.datastructures;

/**
 * Enum class to describe frame sort modes.
 */
public enum FrameSortMode {
    FRAME_COUNT(0),
    DATE(1),
    F_STOP(2),
    SHUTTER_SPEED(3),
    LENS(4);

    int value;

    FrameSortMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FrameSortMode fromValue(int value) {
        switch (value) {
            case 0: default:
                return FRAME_COUNT;
            case 1:
                return DATE;
            case 2:
                return F_STOP;
            case 3:
                return SHUTTER_SPEED;
            case 4:
                return LENS;
        }
    }
}
