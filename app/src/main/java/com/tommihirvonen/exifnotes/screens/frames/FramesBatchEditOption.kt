package com.tommihirvonen.exifnotes.screens.frames

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tommihirvonen.exifnotes.R

enum class FramesBatchEditOption {
    FrameCounts,
    DateAndTime,
    Lens,
    Aperture,
    ShutterSpeed,
    Filters,
    FocalLength,
    ExposureCompensation,
    Location,
    LightSource,
    ReverseFrameCounts;

    val description: String @Composable get() = with (LocalContext.current) {
        when (this@FramesBatchEditOption) {
            FrameCounts -> resources.getString(R.string.EditFrameCounts)
            DateAndTime -> resources.getString(R.string.EditDateAndTime)
            Lens -> resources.getString(R.string.EditLens)
            Aperture -> resources.getString(R.string.EditAperture)
            ShutterSpeed -> resources.getString(R.string.EditShutterSpeed)
            Filters -> resources.getString(R.string.EditFilters)
            FocalLength -> resources.getString(R.string.EditFocalLength)
            ExposureCompensation -> resources.getString(R.string.EditExposureCompensation)
            Location -> resources.getString(R.string.EditLocation)
            LightSource -> resources.getString(R.string.EditLightSource)
            ReverseFrameCounts -> resources.getString(R.string.ReverseFrameCounts)
        }
    }
}