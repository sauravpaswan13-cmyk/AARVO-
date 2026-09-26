package com.aarvo

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The reference itself contains the status/navigation chrome, so the Android
        // system bars are hidden to preserve the supplied 706x1536 composition.
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val bytes = Base64.decode(SplashReferenceImage.WEBP_BASE64, Base64.DEFAULT)
        val image = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO splash screen"
        }
        setContentView(image)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1900L)
    }
}
