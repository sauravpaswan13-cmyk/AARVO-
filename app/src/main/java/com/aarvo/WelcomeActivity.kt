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
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this)
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFFFFFFF.toInt(), 0xFFF7F2FF.toInt(), 0xFFF1F7FF.toInt()))
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(28), dp(42), dp(28), dp(28)) }
        root.addView(content, FrameLayout.LayoutParams(-1, -1))

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo); scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO logo"; setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        content.addView(logo, LinearLayout.LayoutParams(dp(190), dp(190)).apply { topMargin = dp(12) })

        val title = TextView(this).apply {
            text = "AARVO"; textSize = 36f; setTextColor(0xFF25233A.toInt()); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD); letterSpacing = 0.12f
        }
        content.addView(title, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })

        val subtitle = TextView(this).apply {
            text = "Everything you need, beautifully delivered."; textSize = 15f; setTextColor(0xFF6B6878.toInt()); gravity = Gravity.CENTER
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(2); bottomMargin = dp(34) })

        val guest = premiumButton("Continue as Guest", false)
        content.addView(guest, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(14) }); guest.setOnClickListener { enterGuest() }
        val login = premiumButton("Login / Sign Up", true)
        content.addView(login, LinearLayout.LayoutParams(-1, dp(56))); login.setOnClickListener { openLogin() }
        setContentView(root)
    }

    private fun premiumButton(label: String, filled: Boolean): TextView = TextView(this).apply {
        text = label; textSize = 16f; gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(if (filled) Color.WHITE else 0xFF25233A.toInt())
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, if (filled) intArrayOf(0xFF1478F2.toInt(), 0xFF7A2CFF.toInt()) else intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())).apply {
            cornerRadius = dp(18).toFloat(); if (!filled) setStroke(dp(1), 0xFFDDD9EA.toInt())
        }
        isClickable = true; isFocusable = true
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.975f).scaleY(.975f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }; false
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).putString("user_role", "BUYER").remove("auth_token").apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish()
    }
    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).putBoolean("signed_in", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java)); finish()
    }
}
