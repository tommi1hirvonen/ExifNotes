/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.data

import android.database.Cursor
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.extensions.*

internal val filterMapper = { cursor: Cursor ->
    Filter(
        id = cursor.getLong(KEY_FILTER_ID),
        make = cursor.getStringOrNull(KEY_FILTER_MAKE),
        model = cursor.getStringOrNull(KEY_FILTER_MODEL)
    )
}