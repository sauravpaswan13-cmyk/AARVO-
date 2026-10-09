package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Native, responsive Welcome Entry screen. Login and Guest actions are functional. */
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(24), dp(26), dp(26))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(250,252,255), Color.WHITE, Color.rgb(255,249,242), Color.rgb(252,241,255))
            )
        }
        scroll.addView(root)

        val logo = ImageView(this).apply {
            setImageBitmap(LogoUtils.loadTransparentLogo(this@WelcomeActivity))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO original glossy A logo"
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(118), dp(118)).apply { topMargin = dp(4) })

        val brand = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(13, 27, 62))
            textSize = 34f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.015f
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, -2))
        val tagline = label("S H O P   M O R E   •   L I V E   B E T T E R", 11f, Color.rgb(13,27,62), true)
        root.addView(tagline, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })

        val heading = label("Welcome to AARVO", 25f, Color.rgb(13,27,62), true)
        root.addView(heading, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        val subtitle = label("Your trusted online shopping platform\nfor a better tomorrow", 15f, Color.rgb(105,116,145), false)
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })

        val showcase = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(255,251,240), Color.rgb(242,247,255), Color.rgb(255,240,251))
            ).apply { cornerRadius = dp(28).toFloat() }
        }
        val illustration = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        val phone = TextView(this).apply {
            text = "🛍️\n▱\n🛒"
            textSize = 40f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(22,58,150))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(28).toFloat()
                setStroke(dp(2), Color.rgb(220,231,255))
            }
            elevation = dp(6).toFloat()
        }
        illustration.addView(phone, LinearLayout.LayoutParams(dp(120), dp(144)))
        val items = label("🛍️      🎁      🛍️", 34f, Color.BLACK, false)
        illustration.addView(items, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        val chips = label("％   •   ★★★★★   •   ♥", 18f, Color.rgb(234,119,30), true)
        illustration.addView(chips, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
        showcase.addView(illustration, FrameLayout.LayoutParams(-1, -1))
        root.addView(showcase, LinearLayout.LayoutParams(-1, dp(290)).apply { topMargin = dp(22) })

        val login = Button(this).apply {
            text = "Login / Signup   →"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(0,91,255), Color.rgb(0,72,235))
            ).apply { cornerRadius = dp(32).toFloat() }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(24) })

        val guest = Button(this).apply {
            text = "Continue as Guest"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.rgb(0,83,245))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(32).toFloat()
                setStroke(dp(1), Color.rgb(0,91,255))
            }
            setOnClickListener { enterGuest() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(14) })

        setContentView(scroll)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
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
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
