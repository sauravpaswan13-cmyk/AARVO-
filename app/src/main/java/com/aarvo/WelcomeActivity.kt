package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Signature-style AARVO mark inspired by the supplied reference: warm gold/orange left ribbon,
 * pink right ribbon and purple accents, with a clean triangular negative space. */
private class AarvoMarkView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = LinearGradient(0f, h, w, 0f,
            intArrayOf(Color.rgb(255, 194, 18), Color.rgb(255, 132, 24), Color.rgb(238, 48, 111), Color.rgb(74, 38, 196)),
            null, Shader.TileMode.CLAMP)
        paint.strokeWidth = w * .20f
        val left = Path().apply {
            moveTo(w * .20f, h * .83f)
            lineTo(w * .50f, h * .12f)
            lineTo(w * .78f, h * .83f)
        }
        canvas.drawPath(left, paint)
        paint.strokeWidth = w * .15f
        canvas.drawLine(w * .34f, h * .56f, w * .66f, h * .56f, paint)
    }
}

/** Small vector shopping-bag illustration so the entry screen does not depend on emoji fonts. */
private class ShoppingBagsView(context: Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; strokeCap = Paint.Cap.ROUND }
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        fun bag(cx: Float, top: Float, bw: Float, bh: Float, color: Int) {
            fill.color = color
            val body = Path().apply {
                moveTo(cx - bw/2, top + bh*.22f)
                lineTo(cx + bw/2, top + bh*.22f)
                lineTo(cx + bw*.42f, top + bh)
                lineTo(cx - bw*.42f, top + bh)
                close()
            }
            canvas.drawPath(body, fill)
            stroke.color = Color.rgb(82, 42, 130)
            val handle = Path().apply {
                moveTo(cx - bw*.25f, top + bh*.27f)
                cubicTo(cx - bw*.25f, top - bh*.08f, cx + bw*.25f, top - bh*.08f, cx + bw*.25f, top + bh*.27f)
            }
            canvas.drawPath(handle, stroke)
        }
        bag(w*.27f, h*.18f, w*.25f, h*.58f, Color.rgb(255, 170, 28))
        bag(w*.50f, h*.08f, w*.29f, h*.72f, Color.rgb(242, 62, 119))
        bag(w*.73f, h*.22f, w*.24f, h*.54f, Color.rgb(86, 49, 196))
    }
}

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 30, 28, 24)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(AarvoMarkView(this), LinearLayout.LayoutParams(112, 92).apply { gravity = Gravity.CENTER_HORIZONTAL })
        val title = TextView(this).apply {
            text = "AARVO"
            textSize = 30f
            setTextColor(Color.rgb(42, 40, 125))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .03f
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 14f
            setTextColor(Color.rgb(80, 78, 105))
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 4)
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        root.addView(ShoppingBagsView(this), LinearLayout.LayoutParams(250, 155).apply { gravity = Gravity.CENTER_HORIZONTAL })

        val guest = Button(this).apply {
            text = "♙  Continue as Guest"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 18f
                setColor(Color.rgb(83, 34, 211))
            }
            setOnClickListener { enterGuest() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 12 })

        val login = Button(this).apply {
            text = "♙  Login / Sign Up"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(65, 35, 170))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 18f
                setColor(Color.WHITE)
                setStroke(3, Color.rgb(105, 56, 222))
            }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 8 })

        val explore = TextView(this).apply {
            text = "Explore as Guest"
            textSize = 15f
            setTextColor(Color.rgb(72, 35, 176))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 8, 0, 16)
            setOnClickListener { enterGuest() }
        }
        root.addView(explore, LinearLayout.LayoutParams(-1, -2))

        val trust = TextView(this).apply {
            text = "⌖  Secure Shopping     ♢  Trusted Support     ♧  Fast Delivery"
            textSize = 11.5f
            setTextColor(Color.rgb(102, 96, 112))
            gravity = Gravity.CENTER
        }
        root.addView(trust, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
    private fun enterGuest() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).apply()
        startActivity(Intent(this, MainActivity::class.java)); finish()
    }
    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java)); finish()
    }
}
