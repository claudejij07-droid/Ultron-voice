package com.example.ultron.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PixelOrbView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint()
    private val pixelCount = 260
    private val points = mutableListOf<FloatArray>()
    private var time = 0f

    init {
        val rand = Random(42)
        for (i in 0 until pixelCount) {
            val theta = rand.nextFloat() * 2 * Math.PI.toFloat()
            val phi = rand.nextFloat() * Math.PI.toFloat()
            points.add(floatArrayOf(theta, phi, rand.nextFloat()))
        }
        postAnimation()
    }

    private fun postAnimation() {
        postDelayed({
            time += 0.02f
            invalidate()
            postAnimation()
        }, 30)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (width.coerceAtMost(height)) / 2.4f

        for (p in points) {
            val theta = p[0] + time * 0.3f
            val phi = p[1]
            val depthPhase = p[2]

            val x = radius * sin(phi) * cos(theta)
            val y = radius * cos(phi)
            val z = radius * sin(phi) * sin(theta)

            val scale = (z + radius) / (2 * radius)
            val px = cx + x
            val py = cy + y

            val pulse = 0.6f + 0.4f * sin(time * 3f + depthPhase * 10f)
            val alpha = (scale * 255 * pulse).toInt().coerceIn(20, 255)

            paint.color = Color.argb(alpha, 255, 140 + (scale * 80).toInt(), 20)
            val size = 3f + scale * 5f

            canvas.drawRect(px - size / 2, py - size / 2, px + size / 2, py + size / 2, paint)
        }
    }
}
