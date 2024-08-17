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

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.core.serializers.LatLngSerializer
import com.tommihirvonen.exifnotes.core.serializers.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime
import java.util.ArrayList

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
@Keep
data class Frame(
    val id: Long = 0,
    val rollId: Long,
    @EncodeDefault
    val count: Int = 0,
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime = LocalDateTime.now(),
    val shutter: String? = null,
    val aperture: String? = null,
    val note: String? = null,
    @Serializable(with = LatLngSerializer::class)
    val location: LatLng? = null,
    val formattedAddress: String? = null,
    val focalLength: Int = 0,
    val exposureComp: String? = null,
    @EncodeDefault
    val noOfExposures: Int = 1,
    @EncodeDefault
    val flashUsed: Boolean = false,
    val flashPower: String? = null, // not used
    val flashComp: String? = null, // not used
    val meteringMode: Int = 0, // not used
    val pictureFilename: String? = null,
    @EncodeDefault
    val lightSource: LightSource = LightSource.Unknown,
    val lens: Lens? = null,
    val filters: List<Filter> = ArrayList(),
    @Transient
    val pictureFileExists: Boolean = false
) : Parcelable

fun List<Frame>.sorted(context: Context, sortMode: FrameSortMode): List<Frame> =
    sortedWith(sortMode.getComparator(context))