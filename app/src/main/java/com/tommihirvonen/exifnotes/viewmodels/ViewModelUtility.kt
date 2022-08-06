/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.tommihirvonen.exifnotes.R

internal object ViewModelUtility {

    fun getMarkerBitmaps(context: Context): List<Bitmap?> = arrayListOf(
        getMarkerBitmap(context),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_AZURE),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_GREEN),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_ORANGE),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_YELLOW),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_BLUE),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_ROSE),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_CYAN),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_VIOLET),
        getMarkerBitmap(context, BitmapDescriptorFactory.HUE_MAGENTA)
    )

    private fun getMarkerBitmap(context: Context, hue: Float): Bitmap? {
        val bitmap = getMarkerBitmap(context)
        return bitmap?.let { setBitmapHue(it, hue) }
    }

    private fun getMarkerBitmap(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_red) ?: return null
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun setBitmapHue(bitmap: Bitmap, hue: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val hvs = FloatArray(3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hvs)
                hvs[0] = hue
                bitmap.setPixel(x, y, Color.HSVToColor(Color.alpha(pixel), hvs))
            }
        }
        return bitmap
    }
}