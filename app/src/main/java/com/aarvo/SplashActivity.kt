package com.aarvo

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity

/**
 * Runtime splash: uses the supplied reference artwork directly.
 *
 * Important: do not recreate/approximate this artwork with separate logo,
 * gradient, text or progress views. The embedded reference is the source of
 * truth for the splash visual.
 */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this)
        val bytes = Base64.decode(SplashReferenceImage.WEBP_BASE64, Base64.DEFAULT)

        val image = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO supplied splash reference"
        }

        root.addView(image, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2150L)
    }
}
