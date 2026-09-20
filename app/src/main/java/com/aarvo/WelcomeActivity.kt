package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 40, 28, 32)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.WHITE, Color.rgb(248, 244, 255), Color.rgb(242, 238, 255))
            )
        }

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 44f
            setTextColor(Color.rgb(45, 20, 130))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .04f
        }, LinearLayout.LayoutParams(-1, 90))

        root.addView(TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 17f
            setTextColor(Color.rgb(54, 45, 92))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 56))

        val spacer = TextView(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val guest = Button(this).apply {
            text = "Guest"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(91, 22, 232), Color.rgb(137, 30, 242), Color.rgb(232, 47, 151))
            ).apply { cornerRadius = 30f }
            setOnClickListener { enterGuest() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, 64).apply { bottomMargin = 16 })

        val login = Button(this).apply {
            text = "Login / Sign Up"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(83, 28, 196))
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 30f
                setColor(Color.WHITE)
                setStroke(3, Color.rgb(103, 42, 225))
            }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 64))

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", true)
            .putBoolean("signed_in", false)
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
