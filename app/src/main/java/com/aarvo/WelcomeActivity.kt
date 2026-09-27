package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(12, 7, 32), Color.rgb(76, 22, 122), Color.rgb(18, 35, 78))
        )

        val glow = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, 0x553BFFFF, 0x66FF4FCB, Color.TRANSPARENT)
            )
            alpha = .7f
        }
        root.addView(glow, FrameLayout.LayoutParams(-1, 2, Gravity.TOP))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 38, 30, 30)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO logo"
        }
        content.addView(logo, LinearLayout.LayoutParams(170, 170))

        val name = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.WHITE)
            textSize = 42f
            gravity = Gravity.CENTER
            letterSpacing = .10f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        content.addView(name, LinearLayout.LayoutParams(-1, 58))

        val line = TextView(this).apply {
            text = "SHOP SMART   •   LIVE BETTER"
            setTextColor(0xD9FFFFFF.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            letterSpacing = .16f
        }
        content.addView(line, LinearLayout.LayoutParams(-1, 38))

        val scene = TextView(this).apply {
            text = "🛍️   📱   👟     🛒     👜"
            textSize = 32f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 8)
        }
        content.addView(scene, LinearLayout.LayoutParams(-1, 76))

        val hint = TextView(this).apply {
            text = "Choose how you want to enter"
            setTextColor(0xBFFFFFFF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
        }
        content.addView(hint, LinearLayout.LayoutParams(-1, 34))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fun button(label: String, filled: Boolean): TextView {
            return TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 70f
                    if (filled) {
                        setColor(0xFFE94B9A.toInt())
                        setStroke(1, 0x99FFFFFF.toInt())
                    } else {
                        setColor(0x18FFFFFF)
                        setStroke(2, 0x88FFFFFF.toInt())
                    }
                }
                isClickable = true
                isFocusable = true
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(.97f).scaleY(.97f).setDuration(80).start()
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                            v.performClick()
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                            true
                        }
                        else -> false
                    }
                }
            }
        }

        val guest = button("Continue as Guest", true)
        guest.setOnClickListener { enterGuest() }
        actions.addView(guest, LinearLayout.LayoutParams(310, 58).apply { bottomMargin = 14 })

        val login = button("Login / Sign Up", false)
        login.setOnClickListener { openLogin() }
        actions.addView(login, LinearLayout.LayoutParams(310, 58))
        content.addView(actions, LinearLayout.LayoutParams(-1, 130))

        val footer = TextView(this).apply {
            text = "AARVO"
            setTextColor(0x66FFFFFF.toInt())
            textSize = 9f
            letterSpacing = .30f
            gravity = Gravity.CENTER
        }
        content.addView(footer, LinearLayout.LayoutParams(-1, 32))

        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        glow.post {
            glow.translationX = -root.width.toFloat()
            glow.animate().translationX(root.width.toFloat()).setDuration(2400).start()
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
