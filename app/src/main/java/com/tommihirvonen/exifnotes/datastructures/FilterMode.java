package com.tommihirvonen.exifnotes.datastructures;

/**
 * Enum class to describe roll filter modes.
 */
public enum FilterMode {
    ACTIVE(0),
    ARCHIVED(1),
    ALL(2);

    int value;

    FilterMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FilterMode fromValue(int value) {
        switch (value) {
            case 0: default:
                return ACTIVE;
            case 1:
                return ARCHIVED;
            case 2:
                return ALL;
        }
    }
}
