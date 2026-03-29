package com.librefocus.services

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
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
import com.librefocus.ui.limits.BlockedOverlayScreen
import com.librefocus.ui.theme.LibreFocusTheme

class BlockingOverlayManager(private val context: Context, private val onGoHome: () -> Unit) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isOverlayShowing = false
    private var composeView: ComposeView? = null

    // A fake lifecycle owner and saved state registry owner needed for Compose in WindowManager
    private class FakeLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun performRestore() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun performDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    private var fakeLifecycleOwner: FakeLifecycleOwner? = null

    fun show(blockedAppName: String, unblockTimeMessage: String, scheduleInformation: String? = null) {
        if (isOverlayShowing && composeView != null) {
            // Already showing, just update the compose content
            composeView?.setContent {
                LibreFocusTheme {
                    BlockedOverlayScreen(
                        blockedAppName = blockedAppName,
                        unblockTimeMessage = unblockTimeMessage,
                        scheduleInformation = scheduleInformation,
                        onGoHomeClicked = onGoHome
                    )
                }
            }
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        // Remove FLAG_NOT_FOCUSABLE so views can receive touch events normally
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

        composeView = ComposeView(context).apply {
            val lifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner = lifecycleOwner

            lifecycleOwner.performRestore()
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                LibreFocusTheme {
                    BlockedOverlayScreen(
                        blockedAppName = blockedAppName,
                        unblockTimeMessage = unblockTimeMessage,
                        scheduleInformation = scheduleInformation,
                        onGoHomeClicked = onGoHome
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            isOverlayShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        if (isOverlayShowing && composeView != null) {
            try {
                windowManager.removeView(composeView)
                fakeLifecycleOwner?.performDestroy()
                composeView = null
                fakeLifecycleOwner = null
                isOverlayShowing = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
