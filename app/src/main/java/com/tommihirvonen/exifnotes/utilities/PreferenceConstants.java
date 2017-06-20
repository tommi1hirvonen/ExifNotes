package com.tommihirvonen.exifnotes.utilities;

/**
 * Class containing global constants used as keys and values when
 * reading or editing SharedPreferences.
 * Class mimics a static class.
 */
public final class PreferenceConstants {

    static final String VALUE_APP_THEME_LIGHT = "LIGHT";

    static final String VALUE_APP_THEME_DARK = "DARK";

    public static final String KEY_APP_THEME = "AppTheme";

    public static final String KEY_MAP_TYPE = "MAP_TYPE";

    public static final String KEY_UI_COLOR = "UIColor";

    public static final String KEY_GPS_UPDATE = "GPSUpdate";

    public static final String KEY_FRAME_SORT_ORDER = "FrameSortOrder";

    public static final String KEY_ROLL_SORT_ORDER = "RollSortOrder";

    public static final String KEY_FILES_TO_EXPORT = "FilesToExport";

    public static final String VALUE_BOTH = "BOTH";

    public static final String VALUE_CSV = "CSV";

    public static final String VALUE_EXIFTOOL = "EXIFTOOL";

    public static final String KEY_EXPORT_DATABASE = "ExportDatabase";

    public static final String KEY_IMPORT_DATABASE = "ImportDatabase";

    /**
     * Limit instantiation, empty private constructor
     */
    private PreferenceConstants(){

    }

}
