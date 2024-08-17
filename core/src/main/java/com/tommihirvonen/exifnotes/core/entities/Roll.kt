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

@file:UseSerializers(LocalDateTimeSerializer::class)

package com.tommihirvonen.exifnotes.core.entities

import android.os.Parcelable
import androidx.annotation.Keep
import com.tommihirvonen.exifnotes.core.serializers.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
@Keep
data class Roll(
    val id: Long = 0,
    val name: String? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val unloaded: LocalDateTime? = null,
    val developed: LocalDateTime? = null,
    val note: String? = null,
    val camera: Camera? = null,
    val iso: Int = 0,
    val pushPull: String? = null,
    @EncodeDefault
    val format: Format = Format.MM35,
    @EncodeDefault
    val archived: Boolean = false,
    val filmStock: FilmStock? = null,
    @EncodeDefault
    val favorite: Boolean = false,
    val labels: List<Label> = emptyList(),
    val frames: List<Frame> = emptyList(),
    @Transient
    val frameCount: Int = 0
) : Parcelable

fun List<Roll>.sorted(sortMode: RollSortMode): List<Roll> = sortedWith(sortMode.comparator)