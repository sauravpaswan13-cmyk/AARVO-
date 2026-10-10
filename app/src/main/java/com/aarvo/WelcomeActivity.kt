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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Minimal Welcome screen. Visual values live in AarvoScreenDesign; auth and guest
 * navigation stay in this Activity so visual edits do not break app behaviour.
 */
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(AarvoScreenDesign.pageBackground)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                dp(AarvoScreenDesign.PAGE_HORIZONTAL_PADDING_DP),
                dp(AarvoScreenDesign.PAGE_VERTICAL_PADDING_DP),
                dp(AarvoScreenDesign.PAGE_HORIZONTAL_PADDING_DP),
                dp(AarvoScreenDesign.PAGE_VERTICAL_PADDING_DP)
            )
        }
        scroll.addView(root)

        val logo = ImageView(this).apply {
            setImageBitmap(LogoUtils.loadTransparentLogo(this@WelcomeActivity))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO logo"
        }
        root.addView(logo, LinearLayout.LayoutParams(
            dp(AarvoScreenDesign.WELCOME_LOGO_DP),
            dp(AarvoScreenDesign.WELCOME_LOGO_DP)
        ))

        if (AarvoScreenDesign.SHOW_BRAND_TEXT) {
            root.addView(label("AARVO", 32f, AarvoScreenDesign.brandColor, true))
        }
        if (AarvoScreenDesign.SHOW_TAGLINE) {
            root.addView(label("SHOP MORE  |  LIVE BETTER", 12f, AarvoScreenDesign.brandColor, false))
        }

        root.addView(label("Welcome to AARVO", 24f, AarvoScreenDesign.brandColor, true).apply {
            setPadding(0, dp(24), 0, dp(8))
        })
        root.addView(label("Sign in with your mobile number to continue", 15f, Color.DKGRAY, false).apply {
            setPadding(0, 0, 0, dp(24))
        })

        val login = Button(this).apply {
            text = "Login / Sign Up"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(AarvoScreenDesign.primaryButtonStart, AarvoScreenDesign.primaryButtonEnd)
            ).apply { cornerRadius = dp(18).toFloat() }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, dp(AarvoScreenDesign.BUTTON_HEIGHT_DP)))

        val guest = Button(this).apply {
            text = "Continue as Guest"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(AarvoScreenDesign.secondaryBorder)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), AarvoScreenDesign.secondaryBorder)
            }
            setOnClickListener { enterGuest() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, dp(AarvoScreenDesign.BUTTON_HEIGHT_DP)).apply {
            topMargin = dp(AarvoScreenDesign.BUTTON_GAP_DP)
        })

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
