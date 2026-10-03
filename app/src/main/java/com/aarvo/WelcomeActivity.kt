package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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

        val bytes = Base64.decode(WelcomeReferenceImage.WEBP_BASE64, Base64.DEFAULT)
        val image = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO welcome entry"
        }
        root.addView(image, FrameLayout.LayoutParams(-1, -1))

        val guest = View(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Continue as Guest"
            setOnClickListener { enterGuest() }
        }
        root.addReferenceHit(guest, 66f, 1138f, 574f, 116f)

        val login = View(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Login / Signup"
            setOnClickListener { openLogin() }
        }
        root.addReferenceHit(login, 66f, 1280f, 574f, 112f)

        setContentView(root)
    }

    private class ReferenceHitLayout(context: Context) : FrameLayout(context) {
        private data class Hit(val view: View, val x: Float, val y: Float, val w: Float, val h: Float)
        private val hits = mutableListOf<Hit>()

        fun addReferenceHit(view: View, x: Float, y: Float, w: Float, h: Float) {
            hits += Hit(view, x, y, w, h)
            addView(view, LayoutParams(1, 1))
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            val sx = width / 706f
            val sy = height / 1536f
            hits.forEach { hit ->
                val l = (hit.x * sx).toInt()
                val t = (hit.y * sy).toInt()
                val r = ((hit.x + hit.w) * sx).toInt()
                val b = ((hit.y + hit.h) * sy).toInt()
                hit.view.layout(l, t, r, b)
            }
        }
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
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
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
