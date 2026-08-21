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
    private val pixelCount = 700
    private val points = mutableListOf<FloatArray>()
    private var time = 0f
    var isSpeaking = false

    init {
        val rand = Random(42)
        for (i in 0 until pixelCount) {
            val theta = rand.nextFloat() * 2 * Math.PI.toFloat()
            val phi = rand.nextFloat() * Math.PI.toFloat()
            val layer = rand.nextFloat()
            points.add(floatArrayOf(theta, phi, layer))
        }
        postAnimation()
    }

    private fun postAnimation() {
        postDelayed({
            time += if (isSpeaking) 0.05f else 0.02f
            invalidate()
            postAnimation()
        }, 30)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height)) / 2.4f

        val drawList = mutableListOf<FloatArray>()

        for (p in points) {
            val theta = p[0] + time * 0.3f
            val phi = p[1]
            val layerDepth = p[2]
            val radius = baseRadius * (0.7f + layerDepth * 0.3f)

            val x = radius * sin(phi) * cos(theta)
            val y = radius * cos(phi)
            val z = radius * sin(phi) * sin(theta)

            val scale = (z + radius) / (2 * radius)
            val px = cx + x
            val py = cy + y

            drawList.add(floatArrayOf(px, py, scale, layerDepth))
        }

        drawList.sortBy { it[2] }

        for (d in drawList) {
            val px = d[0]
            val py = d[1]
            val scale = d[2]
            val layerDepth = d[3]

            val speakBoost = if (isSpeaking) 1.3f else 1.0f
            val pulse = (0.6f + 0.4f * sin(time * (if (isSpeaking) 6f else 3f) + layerDepth * 10f)) * speakBoost
            val alpha = (scale * 255 * pulse).toInt().coerceIn(15, 255)

            paint.color = Color.argb(alpha, 255, 140 + (scale * 80).toInt(), 20)
            val size = 2.5f + scale * 5f * (0.7f + layerDepth * 0.5f)

            canvas.drawRect(px - size / 2, py - size / 2, px + size / 2, py + size / 2, paint)
        }
    }
}
