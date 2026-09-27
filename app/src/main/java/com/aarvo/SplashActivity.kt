package com.aarvo

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this)

        // The supplied AARVO splash reference is kept in the project and decoded locally.
        val reference = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "AARVO splash"
            try {
                val bytes = Base64.decode(SplashReferenceImage.WEBP_BASE64, Base64.DEFAULT)
                setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Throwable) {
                setBackgroundColor(Color.rgb(16, 8, 40))
            }
        }
        root.addView(reference, FrameLayout.LayoutParams(-1, -1))

        val shade = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0x22000000, 0x08000000, 0x55000000)
            )
        }
        root.addView(shade, FrameLayout.LayoutParams(-1, -1))

        val laser = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, 0x99FF54DD.toInt(), 0xAA62E9FF.toInt(), 0x99FFFFFF.toInt(), Color.TRANSPARENT)
            )
            alpha = .55f
        }
        root.addView(laser, FrameLayout.LayoutParams(110, -1).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        })

        val bottom = FrameLayout(this)
        root.addView(bottom, FrameLayout.LayoutParams(-1, 170).apply {
            gravity = Gravity.BOTTOM
        })

        val track = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(0x55FFFFFF)
                setStroke(1, 0x66FFFFFF)
            }
        }
        val fill = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFFF55D7.toInt(), 0xFF9D5CFF.toInt(), 0xFF55C8FF.toInt(), 0xFFFFD45C.toInt())
            )
        }
        track.addView(fill, FrameLayout.LayoutParams(0, 8, Gravity.CENTER_VERTICAL))
        bottom.addView(track, FrameLayout.LayoutParams(300, 18).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = 48
        })

        val loading = TextView(this).apply {
            text = "Loading AARVO..."
            setTextColor(0xE6FFFFFF.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            letterSpacing = .08f
        }
        bottom.addView(loading, FrameLayout.LayoutParams(-1, 40).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = 82
        })

        setContentView(root)

        root.post {
            ValueAnimator.ofFloat(-140f, root.width + 140f).apply {
                duration = 2100L
                interpolator = LinearInterpolator()
                addUpdateListener { laser.translationX = it.animatedValue as Float }
                start()
            }
            track.post {
                ValueAnimator.ofInt(0, track.width).apply {
                    duration = 1900L
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        fill.layoutParams = fill.layoutParams.apply { width = it.animatedValue as Int }
                        fill.requestLayout()
                    }
                    start()
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(android.content.Intent(this, WelcomeActivity::class.java))
            finish()
        }, 2200L)
    }
}
