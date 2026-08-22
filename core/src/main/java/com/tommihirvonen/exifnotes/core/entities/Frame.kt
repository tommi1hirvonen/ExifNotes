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
import kotlin.math.round
import kotlin.math.roundToInt

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
    val filters: List<Filter> = emptyList(),
    @Transient
    val pictureFileExists: Boolean = false
) : Parcelable

val Frame.accessories: List<Filter> get() = filters.filter(Filter::isAccessory)

val Frame.opticalFilters: List<Filter> get() = filters.filterNot(Filter::isAccessory)

private val Frame.converterFactor: Double get() = accessories.fold(1.0) { value, accessory ->
    when (accessory.type) {
        AttachmentType.Teleconverter, AttachmentType.FocalReducer -> value * accessory.factor
        else -> value
    }
}

val Frame.effectiveFocalLength: Int get() {
    if (focalLength <= 0) return focalLength
    return (focalLength * converterFactor).roundToInt()
}

val Frame.effectiveAperture: Double? get() {
    val apertureValue = aperture?.toDoubleOrNull() ?: return null
    val extension = accessories.sumOf { if (it.type == AttachmentType.ExtensionTube) it.factor else 0.0 }
    if (extension > 0 && focalLength <= 0) return null
    val extensionFactor = if (extension > 0) 1 + extension / focalLength else 1.0
    return round(apertureValue * converterFactor * extensionFactor * 10.0) / 10.0
}

val Frame.effectiveLensModel: String? get() {
    val lensModel = lens?.model ?: return null
    return listOf(lensModel).plus(accessories.map(Filter::name)).joinToString(" + ")
}

fun List<Frame>.sorted(context: Context, sortMode: FrameSortMode): List<Frame> =
    sortedWith(sortMode.getComparator(context))
