package com.tommihirvonen.exifnotes.utilities

import android.animation.ObjectAnimator
import android.transition.TransitionValues
import android.transition.Visibility
import android.view.View
import android.view.ViewGroup

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
            viewTop <= epicenter.top -> sceneRootY - epicenter.top
            else -> sceneRootY + sceneRoot.height - epicenter.bottom
        }
    }
}