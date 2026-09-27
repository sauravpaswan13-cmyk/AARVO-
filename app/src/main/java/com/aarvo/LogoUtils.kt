package com.aarvo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs

object LogoUtils {
    fun loadTransparentLogo(context: Context): Bitmap {
        val source = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.aarvo_logo
        ).copy(Bitmap.Config.ARGB_8888, true)

        val w = source.width
        val h = source.height
        if (w == 0 || h == 0) return source

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val visited = BooleanArray(pixels.size)
        val queue = java.util.ArrayDeque<Int>()

        fun rgbDistance(a: Int, b: Int): Int {
            return abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
                abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
                abs((a and 0xFF) - (b and 0xFF))
        }

        // Remove only the blue area connected to the image edges. This preserves
        // blue portions that are part of the actual AARVO logo.
        val backgroundSeeds = intArrayOf(
            pixels[0],
            pixels[w - 1],
            pixels[(h - 1) * w],
            pixels[h * w - 1]
        )

        fun isBlueBackground(color: Int): Boolean {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            if (b < 100 || b < r + 18 || b < g + 8) return false
            return backgroundSeeds.any { rgbDistance(color, it) <= 105 }
        }

        for (x in 0 until w) {
            val top = x
            val bottom = (h - 1) * w + x
            if (!visited[top] && isBlueBackground(pixels[top])) {
                visited[top] = true
                queue.add(top)
            }
            if (!visited[bottom] && isBlueBackground(pixels[bottom])) {
                visited[bottom] = true
                queue.add(bottom)
            }
        }
        for (y in 0 until h) {
            val left = y * w
            val right = y * w + (w - 1)
            if (!visited[left] && isBlueBackground(pixels[left])) {
                visited[left] = true
                queue.add(left)
            }
            if (!visited[right] && isBlueBackground(pixels[right])) {
                visited[right] = true
                queue.add(right)
            }
        }

        val neighbors = intArrayOf(-1, 1, -w, w)
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            pixels[index] = pixels[index] and 0x00FFFFFF

            val x = index % w
            for (delta in neighbors) {
                val next = index + delta
                if (next < 0 || next >= pixels.size) continue
                if ((delta == -1 && x == 0) || (delta == 1 && x == w - 1)) continue
                if (!visited[next] && isBlueBackground(pixels[next])) {
                    visited[next] = true
                    queue.add(next)
                }
            }
        }

        source.setPixels(pixels, 0, w, 0, 0, w, h)
        return source
    }
}
