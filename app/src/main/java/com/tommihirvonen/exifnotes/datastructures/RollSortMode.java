package com.tommihirvonen.exifnotes.datastructures;

/**
 * Enum class to describe roll sort modes.
 */
public enum RollSortMode {
    DATE(0),
    NAME(1),
    CAMERA(2);

    int value;

    RollSortMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RollSortMode fromValue(int value) {
        switch (value) {
            case 0: default:
                return DATE;
            case 1:
                return NAME;
            case 2:
                return CAMERA;
        }
    }
}
