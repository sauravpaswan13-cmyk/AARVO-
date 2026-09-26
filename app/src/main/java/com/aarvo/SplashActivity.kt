package com.aarvo

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(4, 35, 150))
            clipChildren = true
            clipToPadding = true
        }

        val bytes = Base64.decode(AarvoSplashImage.DATA, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val image = ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO splash screen"
            alpha = 0f
        }
        root.addView(image, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))

        val laser = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(20, 120, 235, 255),
                    Color.argb(180, 255, 255, 255),
                    Color.argb(75, 80, 220, 255),
                    Color.TRANSPARENT
                )
            )
            alpha = 0.58f
            rotation = 18f
            elevation = dp(8).toFloat()
        }
        root.addView(laser, FrameLayout.LayoutParams(dp(72), -1, Gravity.CENTER_VERTICAL))

        setContentView(root)

        image.animate().alpha(1f).setDuration(350).start()

        root.post {
            val start = -root.width.toFloat() - dp(120)
            val end = root.width.toFloat() + dp(120)
            laser.translationX = start
            laser.animate()
                .translationX(end)
                .setDuration(1050)
                .setStartDelay(250)
                .withEndAction {
                    laser.translationX = start
                    laser.animate()
                        .translationX(end)
                        .setDuration(1050)
                        .setStartDelay(150)
                        .start()
                }
                .start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1900)
    }
}
