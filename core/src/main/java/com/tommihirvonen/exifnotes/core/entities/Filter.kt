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

package com.tommihirvonen.exifnotes.core.entities

import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class AttachmentType(val usesFactor: Boolean = false) {
    // Filter must remain first: its ordinal is the database migration default.
    Filter,
    Accessory,
    Teleconverter(true),
    FocalReducer(true),
    ExtensionTube(true);

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.ordinal == value } ?: Filter
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
@Keep
data class Filter(
        override val id: Long = 0,
        override val make: String? = null,
        override val model: String? = null,
        @EncodeDefault
        val type: AttachmentType = AttachmentType.Filter,
        @EncodeDefault
        val factor: Double = 1.0,
        @Transient
        val lensIds: HashSet<Long> = HashSet()
) : Gear(), Comparable<Gear> {
    val isAccessory: Boolean get() = type != AttachmentType.Filter
}
