package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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

        val root = ReferenceHitLayout(this)

        // The supplied reference artwork is retained exactly, including the AARVO A logo.
        val bytes = Base64.decode(WelcomeReferenceImage.WEBP_BASE64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("AARVO welcome image could not be decoded")
        val image = android.widget.ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO premium welcome entry"
        }
        root.addView(image, FrameLayout.LayoutParams(-1, -1))

        // Premium, spacious action surface; artwork/logo remains untouched underneath.
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fun premiumButton(text: String): Button = Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            stateListAnimator = null
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 54f
                setColor(0xCC16132A.toInt())
                setStroke(2, 0x55FFFFFF)
            }
        }

        val guest = premiumButton("Continue as Guest")
        guest.setOnClickListener { enterGuest() }
        actions.addView(guest, LinearLayout.LayoutParams(330, 58).apply { bottomMargin = 14 })

        val login = premiumButton("Login / Signup")
        login.setOnClickListener { openLogin() }
        actions.addView(login, LinearLayout.LayoutParams(330, 58))

        root.addView(actions, FrameLayout.LayoutParams(330, 130).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 72
        })

        setContentView(root)
    }

    private class ReferenceHitLayout(context: Context) : FrameLayout(context)

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
