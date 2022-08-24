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

package com.tommihirvonen.exifnotes.utilities

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionValues
import androidx.transition.Visibility

/**
 * Transition which animates views above the epicenter to translate upwards
 * and views below the epicenter to translate downwards.
 */
class SeparateVertical : Visibility() {

    companion object {
        private const val KEY_VIEW_TOP = "VIEW_TOP"
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        super.captureStartValues(transitionValues)
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        super.captureEndValues(transitionValues)
        captureValues(transitionValues)
    }

    override fun onAppear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ) =
        endValues?.let {
            val top = it.values[KEY_VIEW_TOP] as Int
            val endY = view.translationY
            val startY = endY + calculateTranslationDistance(sceneRoot, top)
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
        }

    override fun onDisappear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ) =
        startValues?.let {
            val top = it.values[KEY_VIEW_TOP] as Int
            val startY = view.translationY
            val endY = startY + calculateTranslationDistance(sceneRoot, top)
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
        }

    private fun captureValues(transitionValues: TransitionValues) {
        val (_, top) = IntArray(2).also { transitionValues.view.getLocationOnScreen(it) }
        transitionValues.values[KEY_VIEW_TOP] = top
    }

    private fun calculateTranslationDistance(sceneRoot: View, viewTop: Int): Int {
        val (_, sceneRootY) = IntArray(2).also { sceneRoot.getLocationOnScreen(it) }
        return when {
            epicenter == null -> -sceneRoot.height
            viewTop <= (epicenter?.top ?: 0) -> sceneRootY - (epicenter?.top ?: 0)
            else -> sceneRootY + sceneRoot.height - (epicenter?.bottom ?: 0)
        }
    }
}