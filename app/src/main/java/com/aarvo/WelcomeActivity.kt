package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(9, 7, 25), Color.rgb(35, 12, 60), Color.rgb(7, 22, 50))
            )
        }

        // Fresh welcome screen: reuse the existing AARVO logo asset without modifying it.
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo_webp)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO"
        }
        root.addView(logo, FrameLayout.LayoutParams(190, 190).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 76
        })

        val welcome = TextView(this).apply {
            text = "WELCOME"
            setTextColor(Color.WHITE)
            textSize = 26f
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
        }
        root.addView(welcome, FrameLayout.LayoutParams(-1, 52).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 290
        })

        val subtitle = TextView(this).apply {
            text = "Your everyday shopping, beautifully simple."
            setTextColor(0xB8FFFFFF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, FrameLayout.LayoutParams(-1, 44).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 338
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fun premiumButton(label: String, filled: Boolean): Button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            stateListAnimator = null
            elevation = 0f
            background = GradientDrawable().apply {
                cornerRadius = 60f
                if (filled) {
                    setColor(0xFFE94B9A.toInt())
                    setStroke(1, 0x55FFFFFF)
                } else {
                    setColor(0x1FFFFFFF)
                    setStroke(2, 0x77FFFFFF)
                }
            }
        }

        val guest = premiumButton("Continue as Guest", true)
        guest.setOnClickListener { enterGuest() }
        actions.addView(guest, LinearLayout.LayoutParams(310, 60).apply { bottomMargin = 16 })

        val login = premiumButton("Login / Signup", false)
        login.setOnClickListener { openLogin() }
        actions.addView(login, LinearLayout.LayoutParams(310, 60))

        root.addView(actions, FrameLayout.LayoutParams(310, 136).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 86
        })

        val footer = TextView(this).apply {
            text = "AARVO"
            setTextColor(0x66FFFFFF.toInt())
            textSize = 9f
            letterSpacing = 0.30f
            gravity = Gravity.CENTER
        }
        root.addView(footer, FrameLayout.LayoutParams(-1, 30).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 36
        })

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
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun openLogin() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", false)
            .apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
