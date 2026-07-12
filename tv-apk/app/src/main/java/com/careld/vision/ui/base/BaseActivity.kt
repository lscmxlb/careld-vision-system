package com.careld.vision.ui.base

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
nimport androidx.core.view.WindowInsetsCompat
import timber.log.Timber

/**
 * Base activity for TV
 * 
 * Provides common functionality for all TV activities.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected abstract val layoutResId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId)
        
        setupWindowInsets()
        setupFocusHandling()
        initViews()
        observeData()
    }

    /**
     * Setup window insets for TV
     */
    protected open fun setupWindowInsets() {
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Setup focus handling for TV navigation
     */
    protected open fun setupFocusHandling() {
        // Override in subclasses if needed
    }

    /**
     * Initialize views
     */
    protected abstract fun initViews()

    /**
     * Observe data
     */
    protected abstract fun observeData()

    /**
     * Handle back key
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                onBackPressed()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * Log lifecycle events
     */
    override fun onResume() {
        super.onResume()
        Timber.d("${javaClass.simpleName} onResume")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("${javaClass.simpleName} onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("${javaClass.simpleName} onDestroy")
    }
}
