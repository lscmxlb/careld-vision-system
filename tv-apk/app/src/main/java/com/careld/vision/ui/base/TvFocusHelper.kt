package com.careld.vision.ui.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * TV Focus helper
 * 
 * Manages focus animations and navigation for TV remote control.
 */
object TvFocusHelper {

    private const val SCALE_FACTOR = 1.1f
    private const val ANIMATION_DURATION = 150L

    /**
     * Apply focus animation to view
     */
    fun applyFocusAnimation(view: View, hasFocus: Boolean) {
        view.animate()
            .scaleX(if (hasFocus) SCALE_FACTOR else 1.0f)
            .scaleY(if (hasFocus) SCALE_FACTOR else 1.0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Apply visual feedback
        view.isSelected = hasFocus
    }

    /**
     * Setup focus chain for a list of views
     */
    fun setupFocusChain(views: List<View>) {
        views.forEachIndexed { index, view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true

            // Set focus change listener
            view.setOnFocusChangeListener { v, hasFocus ->
                applyFocusAnimation(v, hasFocus)
            }

            // Set next focus IDs
            val nextIndex = (index + 1) % views.size
            val prevIndex = if (index == 0) views.size - 1 else index - 1

            view.nextFocusForwardId = views[nextIndex].id
            view.nextFocusDownId = views[nextIndex].id
            view.nextFocusUpId = views[prevIndex].id
        }
    }

    /**
     * Setup grid focus navigation
     */
    fun setupGridFocus(
        views: List<View>,
        columns: Int
    ) {
        views.forEachIndexed { index, view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true

            view.setOnFocusChangeListener { v, hasFocus ->
                applyFocusAnimation(v, hasFocus)
            }

            val row = index / columns
            val col = index % columns

            // Calculate neighbors
            val upIndex = if (row > 0) index - columns else -1
            val downIndex = if (row < (views.size - 1) / columns) {
                val nextRowIndex = index + columns
                if (nextRowIndex < views.size) nextRowIndex else -1
            } else -1
            val leftIndex = if (col > 0) index - 1 else -1
            val rightIndex = if (col < columns - 1 && index < views.size - 1) index + 1 else -1

            // Set focus IDs
            if (upIndex >= 0) view.nextFocusUpId = views[upIndex].id
            if (downIndex >= 0) view.nextFocusDownId = views[downIndex].id
            if (leftIndex >= 0) view.nextFocusLeftId = views[leftIndex].id
            if (rightIndex >= 0) view.nextFocusRightId = views[rightIndex].id
        }
    }

    /**
     * Animate focus enter
     */
    fun animateFocusEnter(view: View) {
        view.animate()
            .scaleX(SCALE_FACTOR)
            .scaleY(SCALE_FACTOR)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Animate focus exit
     */
    fun animateFocusExit(view: View) {
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Pulse animation for important elements
     */
    fun pulseAnimation(view: View) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.95f)
        
        scaleDown.duration = 200
        scaleDownY.duration = 200
        
        scaleDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1.0f)
                val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1.0f)
                scaleUp.duration = 200
                scaleUpY.duration = 200
                scaleUp.start()
                scaleUpY.start()
            }
        })
        
        scaleDown.start()
        scaleDownY.start()
    }
}
