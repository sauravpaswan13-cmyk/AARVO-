package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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

        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(248, 247, 252)) }

        val topBand = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(91,33,214), Color.rgb(255,85,215), Color.rgb(85,200,255), Color.rgb(255,212,92))
            )
        }
        root.addView(topBand, FrameLayout.LayoutParams(-1, 5, Gravity.TOP))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 28)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO logo"
        }
        content.addView(logo, LinearLayout.LayoutParams(112, 112).apply { bottomMargin = 10 })

        val name = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(91,33,214))
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = .08f
            gravity = Gravity.CENTER
        }
        content.addView(name, LinearLayout.LayoutParams(-1, 46).apply { bottomMargin = 10 })

        val title = TextView(this).apply {
            text = "Welcome"
            setTextColor(Color.rgb(28,24,34))
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        content.addView(title, LinearLayout.LayoutParams(-1, 38).apply { bottomMargin = 6 })

        val subtitle = TextView(this).apply {
            text = "Discover products you love, all in one place."
            setTextColor(Color.rgb(98,91,109))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, 32).apply { bottomMargin = 14 })

        val divider = View(this).apply { setBackgroundColor(Color.rgb(226,221,234)) }
        content.addView(divider, LinearLayout.LayoutParams(180, 1).apply { bottomMargin = 16 })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fun button(label: String, filled: Boolean): TextView = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(if (filled) Color.WHITE else Color.rgb(91,33,214))
            background = GradientDrawable().apply {
                cornerRadius = 18f
                if (filled) setColor(Color.rgb(91,33,214))
                else { setColor(Color.WHITE); setStroke(2, Color.rgb(91,33,214)) }
            }
            elevation = 3f
            isClickable = true
            isFocusable = true
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { v.animate().scaleX(.98f).scaleY(.98f).setDuration(70).start(); false }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(90).start(); false }
                    else -> false
                }
            }
        }

        val guest = button("Continue as Guest", true)
        guest.setOnClickListener { enterGuest() }
        actions.addView(guest, LinearLayout.LayoutParams(-1, 54).apply { bottomMargin = 12 })

        val login = button("Login / Sign Up", false)
        login.setOnClickListener { openLogin() }
        actions.addView(login, LinearLayout.LayoutParams(-1, 54))
        content.addView(actions, LinearLayout.LayoutParams(-1, 120))

        val footer = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(150,143,160))
            textSize = 10f
            letterSpacing = .22f
            gravity = Gravity.CENTER
        }
        content.addView(footer, LinearLayout.LayoutParams(-1, 30).apply { topMargin = 10 })

        root.addView(content, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        setContentView(root)
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
