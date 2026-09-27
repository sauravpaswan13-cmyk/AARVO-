package com.aarvo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs

object LogoUtils {
    fun loadTransparentLogo(context: Context): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            // The logo is displayed small; sampling prevents a large source bitmap
            // from allocating several full-size pixel/visited arrays on low-memory phones.
            inSampleSize = 2
        }

        val source = (BitmapFactory.decodeResource(
            context.resources,
            R.drawable.aarvo_logo,
            options
        ) ?: BitmapFactory.decodeResource(context.resources, R.drawable.aarvo_logo))
            .copy(Bitmap.Config.ARGB_8888, true)

        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val visited = BooleanArray(pixels.size)
        // Primitive queue avoids boxing every pixel into Integer objects.
        val queue = IntArray(pixels.size)
        var head = 0
        var tail = 0

        fun rgbDistance(a: Int, b: Int): Int {
            return abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
                abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
                abs((a and 0xFF) - (b and 0xFF))
        }

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

        fun enqueue(index: Int) {
            if (!visited[index]) {
                visited[index] = true
                if (tail < queue.size) queue[tail++] = index
            }
        }

        for (x in 0 until w) {
            val top = x
            val bottom = (h - 1) * w + x
            if (isBlueBackground(pixels[top])) enqueue(top)
            if (isBlueBackground(pixels[bottom])) enqueue(bottom)
        }
        for (y in 0 until h) {
            val left = y * w
            val right = y * w + (w - 1)
            if (isBlueBackground(pixels[left])) enqueue(left)
            if (isBlueBackground(pixels[right])) enqueue(right)
        }

        while (head < tail) {
            val index = queue[head++]
            pixels[index] = pixels[index] and 0x00FFFFFF

            val x = index % w

            if (x > 0) {
                val next = index - 1
                if (!visited[next] && isBlueBackground(pixels[next])) enqueue(next)
            }
            if (x < w - 1) {
                val next = index + 1
                if (!visited[next] && isBlueBackground(pixels[next])) enqueue(next)
            }
            if (index >= w) {
                val next = index - w
                if (!visited[next] && isBlueBackground(pixels[next])) enqueue(next)
            }
            if (index < pixels.size - w) {
                val next = index + w
                if (!visited[next] && isBlueBackground(pixels[next])) enqueue(next)
            }
        }

        source.setPixels(pixels, 0, w, 0, 0, w, h)
        return source
    }
}
