package com.tommihirvonen.exifnotes.utilities

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.transition.TransitionValues
import android.transition.Visibility
import android.view.View
import android.view.ViewGroup

/**
 * A simple Transition which allows the views above the epic centre to transition upwards and views
 * below the epic centre to transition downwards.
 */
class SeparateVertical : Visibility() {

    companion object {
        private const val KEY_SCREEN_BOUNDS = "SCREEN_BOUNDS"
    }

    private val location = IntArray(2)

    override fun captureStartValues(transitionValues: TransitionValues) {
        super.captureStartValues(transitionValues)
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        super.captureEndValues(transitionValues)
        captureValues(transitionValues)
    }

    override fun onAppear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues?, endValues: TransitionValues?) =
        endValues?.let {
            // Calculate target values in the end scene and provide
            val bounds = it.values[KEY_SCREEN_BOUNDS] as Rect
            val endY = view.translationY
            val startY = endY + calculateDistance(sceneRoot, bounds)
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
        }

    override fun onDisappear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues?, endValues: TransitionValues?) =
        startValues?.let {
            val bounds = it.values[KEY_SCREEN_BOUNDS] as Rect
            val startY = view.translationY
            val endY = startY + calculateDistance(sceneRoot, bounds)
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
        }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        view.getLocationOnScreen(location)
        val (left, top) = location
        val right = left + view.width
        val bottom = top + view.height
        transitionValues.values[KEY_SCREEN_BOUNDS] = Rect(left, top, right, bottom)
    }

    private fun calculateDistance(sceneRoot: View, viewBounds: Rect): Int {
        sceneRoot.getLocationOnScreen(location)
        val sceneRootY = location[1]
        return when {
            epicenter == null -> -sceneRoot.height
            viewBounds.top <= epicenter.top -> sceneRootY - epicenter.top
            else -> sceneRootY + sceneRoot.height - epicenter.bottom
        }
    }
}
