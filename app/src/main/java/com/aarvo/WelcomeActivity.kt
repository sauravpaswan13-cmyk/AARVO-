package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }

        val bytes = Base64.decode(WelcomeReferenceImage.WEBP_BASE64, Base64.DEFAULT)
        val artwork = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            // Avoid Android 10 GPU texture corruption while preserving the supplied artwork.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO Welcome"
        }
        root.addView(artwork, FrameLayout.LayoutParams(-1, -1))

        // Transparent hit areas keep the supplied artwork exactly as-is while
        // making its Guest and Login buttons functional.
        root.post {
            val w = root.width
            val h = root.height
            root.addView(hitArea(0.12f, 0.715f, 0.88f, 0.795f, w, h) { enterGuest() })
            root.addView(hitArea(0.12f, 0.835f, 0.88f, 0.915f, w, h) { openLogin() })
        }

        setContentView(root)
    }

    private fun hitArea(
        left: Float, top: Float, right: Float, bottom: Float,
        parentW: Int, parentH: Int, action: () -> Unit
    ): View {
        return View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.alpha = 0.72f
                    MotionEvent.ACTION_UP -> { v.alpha = 1f; v.performClick() }
                    MotionEvent.ACTION_CANCEL -> v.alpha = 1f
                }
                true
            }
            setOnClickListener { action() }
            layoutParams = FrameLayout.LayoutParams(
                ((right - left) * parentW).toInt(),
                ((bottom - top) * parentH).toInt()
            ).apply {
                leftMargin = (left * parentW).toInt()
                topMargin = (top * parentH).toInt()
            }
        }
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true)
            .putBoolean("signed_in", false).putString("user_role", "BUYER")
            .remove("auth_token").apply()
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false)
            .putBoolean("signed_in", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
