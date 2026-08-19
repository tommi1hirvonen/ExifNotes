package com.tommihirvonen.exifnotes.core.entities

import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val offsetFormatter = DateTimeFormatter.ofPattern("xxx")

/** EXIF offsets use signed hours and minutes, including +00:00 for UTC. */
val Frame.utcOffset: String? get() = utcOffsetSeconds?.let { seconds ->
    // EXIF cannot represent historical offsets with a seconds component.
    if (seconds % 60 != 0) return@let null
    try {
        offsetFormatter.format(ZoneOffset.ofTotalSeconds(seconds))
    } catch (_: DateTimeException) {
        null
    }
}

fun Frame.withDate(value: LocalDateTime): Frame = copy(date = value).withTimeZone(timeZoneId)

/** Keep the entered wall time and resolve the offset using the rules on that date. */
fun Frame.withTimeZone(value: String?): Frame {
    val offsets = try {
        value?.let { ZoneId.of(it).rules.getValidOffsets(date) }.orEmpty()
    } catch (_: DateTimeException) {
        emptyList()
    }
    // During a repeated hour, retain a previously known offset if possible; otherwise use
    // the earlier occurrence. A skipped hour has no valid offset and remains unspecified.
    val preferred = offsets.firstOrNull {
        value == timeZoneId && it.totalSeconds == utcOffsetSeconds
    }
    return copy(timeZoneId = value, utcOffsetSeconds = (preferred ?: offsets.firstOrNull())?.totalSeconds)
}
