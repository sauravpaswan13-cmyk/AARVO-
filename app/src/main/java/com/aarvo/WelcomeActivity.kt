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
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

private class AarvoMarkView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = LinearGradient(0f, h, w, 0f, intArrayOf(Color.rgb(255,194,18), Color.rgb(255,132,24), Color.rgb(238,48,111), Color.rgb(74,38,196)), null, Shader.TileMode.CLAMP)
        paint.strokeWidth = w * .20f
        val p = Path().apply { moveTo(w*.20f,h*.83f); lineTo(w*.50f,h*.12f); lineTo(w*.78f,h*.83f) }
        canvas.drawPath(p, paint); paint.strokeWidth = w*.15f; canvas.drawLine(w*.34f,h*.56f,w*.66f,h*.56f,paint)
    }
}

private class ShoppingBagsView(context: Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; strokeCap = Paint.Cap.ROUND }
    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        fun bag(cx:Float,top:Float,bw:Float,bh:Float,c:Int){
            fill.color=c
            val body=Path().apply{moveTo(cx-bw/2,top+bh*.22f);lineTo(cx+bw/2,top+bh*.22f);lineTo(cx+bw*.42f,top+bh);lineTo(cx-bw*.42f,top+bh);close()}
            canvas.drawPath(body,fill)
            stroke.color=Color.rgb(82,42,130)
            val handle=Path().apply{moveTo(cx-bw*.25f,top+bh*.27f);cubicTo(cx-bw*.25f,top-bh*.08f,cx+bw*.25f,top-bh*.08f,cx+bw*.25f,top+bh*.27f)}
            canvas.drawPath(handle,stroke)
        }
        bag(w*.27f,h*.18f,w*.25f,h*.58f,Color.rgb(255,170,28))
        bag(w*.50f,h*.08f,w*.29f,h*.72f,Color.rgb(242,62,119))
        bag(w*.73f,h*.22f,w*.24f,h*.54f,Color.rgb(86,49,196))
    }
}

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 24, 28, 26)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(AarvoMarkView(this), LinearLayout.LayoutParams(96, 70).apply { gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(TextView(this).apply {
            text = "AARVO"; textSize = 30f; setTextColor(Color.rgb(42,40,125)); gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD); letterSpacing = .04f
        }, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply {
            text = "Your One Stop Shopping Destination"; textSize = 14f; setTextColor(Color.rgb(80,78,105)); gravity = Gravity.CENTER; setPadding(0, 5, 0, 2)
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(ShoppingBagsView(this), LinearLayout.LayoutParams(250, 155).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = 10; bottomMargin = 6 })

        // Reference-style welcome screen: guest browsing first, login available separately.
        val guest = Button(this).apply {
            text = "Continue as Guest"; textSize = 16f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.WHITE); isAllCaps = false; minHeight = 56
            background = GradientDrawable().apply { cornerRadius = 18f; setColor(Color.rgb(83,34,211)) }
            setOnClickListener { enterApp() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 10 })

        val login = Button(this).apply {
            text = "Login / Sign Up"; textSize = 16f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.rgb(65,35,170)); isAllCaps = false; minHeight = 56
            background = GradientDrawable().apply { cornerRadius = 18f; setColor(Color.WHITE); setStroke(3, Color.rgb(105,56,222)) }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 10 })

        root.addView(TextView(this).apply {
            text = "Explore freely • Login when you want to buy or use account features"; textSize = 12f; setTextColor(Color.rgb(102,96,112)); gravity = Gravity.CENTER; setPadding(4, 0, 4, 0)
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Best Prices   •   Secure Shopping   •   Fast Delivery"; textSize = 11f; setTextColor(Color.rgb(91,78,140)); gravity = Gravity.CENTER; setPadding(0, 14, 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterApp() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).apply()
        startActivity(Intent(this, MainActivity::class.java)); finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java)); finish()
    }
}
