package com.mliem.scientific_calculator

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mliem.scientific_calculator.data.Settings
import com.mliem.scientific_calculator.ui.FloatingCalculator

/** Hosts [FloatingCalculator] in an overlay window; it needs the "display over other apps" permission. */
class FloatingCalculatorService : Service() {
    private val owner = OverlayOwner()
    private lateinit var windowManager: WindowManager
    private var overlay: ComposeView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        owner.resume()
        show()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    private fun show() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24.dp()
            y = 120.dp()
        }
        val settings = (application as CalculatorApp).settings.settings
        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setContent {
            val current by settings.collectAsState(initial = Settings())
            FloatingCalculator(
                settings = current,
                onClose = ::stopSelf,
                onFullScreen = {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    stopSelf()
                },
                onDrag = { dx, dy ->
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    windowManager.updateViewLayout(view, params)
                },
                onTransparency = { translucent ->
                    params.alpha = if (translucent) 0.65f else 1f
                    windowManager.updateViewLayout(view, params)
                },
            )
        }
        windowManager.addView(view, params)
        overlay = view
    }

    override fun onDestroy() {
        overlay?.let { windowManager.removeView(it) }
        overlay = null
        owner.destroy()
        super.onDestroy()
    }

    /** A ComposeView outside an activity needs these three owners on its view tree. */
    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)
        override val viewModelStore = ViewModelStore()
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

        init {
            savedState.performAttach()
            savedState.performRestore(null)
        }

        fun resume() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}
