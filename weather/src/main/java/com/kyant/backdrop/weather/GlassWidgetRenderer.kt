package com.kyant.backdrop.weather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import androidx.annotation.DrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pure-Canvas renderer for the weather widget.
 *
 * v3 — Fixed:
 * 1. Widget too narrow → use 800×400 (2:1 ratio matching 4×2 cells)
 * 2. Opaque placeholder → draw translucent glass when no cache
 * 3. Aesthetics → better font sizes, spacing, and layout proportions
 */
object GlassWidgetRenderer {

    private const val CORNER_RADIUS = 32f

    suspend fun render(context: Context, widthPx: Int = 800, heightPx: Int = 400): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val weather = WeatherRepository.loadCachedOnly(context)
                if (weather == null) {
                    return@withContext createGlassPlaceholder(widthPx, heightPx)
                }

                val bgResId = WeatherBackgroundProvider.getBackgroundResourceOrNull(weather.weatherIcon)
                val bgBitmap = decodeAndScaleBitmap(context, bgResId, widthPx, heightPx)
                if (bgBitmap == null) {
                    return@withContext createGlassPlaceholder(widthPx, heightPx)
                }

                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val w = widthPx.toFloat()
                val h = heightPx.toFloat()

                // ── Clip to rounded rect ──
                canvas.clipPath(Path().apply {
                    addRoundRect(RectF(0f, 0f, w, h), CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
                })

                // 1. Blurred background
                val blurred = applyBoxBlur(bgBitmap, radius = 12, passes = 3)
                canvas.drawBitmap(blurred, 0f, 0f, null)

                // 2. Glass overlay
                drawGlassOverlay(canvas, w, h)

                // 3. Highlight
                drawHighlight(canvas, w, h)

                // 4. Text
                drawWeatherContent(canvas, weather, w, h)

                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                createGlassPlaceholder(widthPx, heightPx)
            }
        }
    }

    // ── Background decoding ─────────────────────────────────────────────

    private fun decodeAndScaleBitmap(
        context: Context,
        @DrawableRes resId: Int,
        targetW: Int,
        targetH: Int
    ): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, opts)
            val srcW = opts.outWidth
            val srcH = opts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sampleSize = 1
            while (srcW / sampleSize > targetW * 2 || srcH / sampleSize > targetH * 2) {
                sampleSize *= 2
            }
            opts.inJustDecodeBounds = false
            opts.inSampleSize = sampleSize
            val sampled = BitmapFactory.decodeResource(context.resources, resId, opts) ?: return null

            val scale = maxOf(targetW.toFloat() / sampled.width, targetH.toFloat() / sampled.height)
            val scaledW = (sampled.width * scale).toInt()
            val scaledH = (sampled.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(sampled, scaledW, scaledH, true)
            val offsetX = (scaledW - targetW) / 2
            val offsetY = (scaledH - targetH) / 2
            val cropped = Bitmap.createBitmap(scaled, offsetX, offsetY, targetW, targetH)

            if (scaled !== sampled) sampled.recycle()
            if (scaled !== cropped) scaled.recycle()
            cropped
        } catch (e: Exception) {
            null
        }
    }

    // ── Box Blur ───────────────────────────────────────────────────────

    private fun applyBoxBlur(src: Bitmap, radius: Int, passes: Int): Bitmap {
        val w = src.width
        val h = src.height
        var pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        var buffer = IntArray(w * h)

        for (pass in 0 until passes) {
            boxBlurHorizontal(pixels, buffer, w, h, radius)
            boxBlurVertical(buffer, pixels, w, h, radius)
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            var a = 0; var rSum = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val px = src[y * w + (i.coerceIn(0, w - 1))]
                a += (px shr 24) and 0xFF; rSum += (px shr 16) and 0xFF
                g += (px shr 8) and 0xFF; b += px and 0xFF
            }
            for (x in 0 until w) {
                dst[y * w + x] = Color.argb(
                    (a / div).coerceIn(0, 255), (rSum / div).coerceIn(0, 255),
                    (g / div).coerceIn(0, 255), (b / div).coerceIn(0, 255)
                )
                val remove = src[y * w + (x - r).coerceIn(0, w - 1)]
                val add = src[y * w + (x + r + 1).coerceIn(0, w - 1)]
                a += ((add shr 24) and 0xFF) - ((remove shr 24) and 0xFF)
                rSum += ((add shr 16) and 0xFF) - ((remove shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((remove shr 8) and 0xFF)
                b += (add and 0xFF) - (remove and 0xFF)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var a = 0; var rSum = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val px = src[(i.coerceIn(0, h - 1)) * w + x]
                a += (px shr 24) and 0xFF; rSum += (px shr 16) and 0xFF
                g += (px shr 8) and 0xFF; b += px and 0xFF
            }
            for (y in 0 until h) {
                dst[y * w + x] = Color.argb(
                    (a / div).coerceIn(0, 255), (rSum / div).coerceIn(0, 255),
                    (g / div).coerceIn(0, 255), (b / div).coerceIn(0, 255)
                )
                val remove = src[((y - r).coerceIn(0, h - 1)) * w + x]
                val add = src[((y + r + 1).coerceIn(0, h - 1)) * w + x]
                a += ((add shr 24) and 0xFF) - ((remove shr 24) and 0xFF)
                rSum += ((add shr 16) and 0xFF) - ((remove shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((remove shr 8) and 0xFF)
                b += (add and 0xFF) - (remove and 0xFF)
            }
        }
    }

    // ── Glass Effect ───────────────────────────────────────────────────

    private fun drawGlassOverlay(canvas: Canvas, w: Float, h: Float) {
        val inset = 4f
        val rect = RectF(inset, inset, w - inset, h - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(12, 255, 255, 255) // ~5% white overlay — more subtle
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, CORNER_RADIUS - inset, CORNER_RADIUS - inset, paint)
    }

    private fun drawHighlight(canvas: Canvas, w: Float, h: Float) {
        val top = h * 0.10f
        val bottom = h * 0.30f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, top, 0f, bottom,
                Color.argb(25, 255, 255, 255), // ~10% → 0%
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, top, w, bottom, paint)
    }

    // ── Weather Text ──────────────────────────────────────────────────

    private fun drawWeatherContent(canvas: Canvas, weather: WeatherResult, w: Float, h: Float) {
        val padL = 28f
        val padR = 28f
        val padT = 24f
        val padB = 18f

        // ── City name (22sp, semi-bold) ──
        val cityPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = 22f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        var y = padT + 22f
        canvas.drawText(weather.cityName, padL, y, cityPaint)

        // ── Temperature (56sp, bold) ──
        val tempPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        y += 12f + 48f  // larger gap + temp baseline offset
        val tempText = "${weather.currentTemp}°"
        canvas.drawText(tempText, padL, y, tempPaint)

        // ── Description + high/low (16sp) ──
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = 16f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        y += 8f + 14f
        val daily = weather.dailyForecasts.firstOrNull()
        val highLow = daily?.let { "${it.tempMax}° / ${it.tempMin}°" } ?: ""
        val descText = "${weather.weatherDescription}  $highLow"
        canvas.drawText(descText, padL, y, descPaint)

        // ── Separator line between main info and hourly forecast ──
        val hours = weather.hourlyForecasts.take(7)
        if (hours.isNotEmpty()) {
            val sepY = y + 16f
            val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(35, 255, 255, 255)
                strokeWidth = 1f
            }
            canvas.drawLine(padL, sepY, w - padR, sepY, sepPaint)

            // ── Hourly forecast (bottom strip) ──
            // 14sp for time, 16sp for temp
            val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(160, 255, 255, 255)
                textSize = 14f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val hTempPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 16f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Anchor hourly to bottom
            val tempY = h - padB
            val timeBaseline = tempY - 2f - 13f
            val cellW = (w - padL - padR) / hours.size

            for ((i, forecast) in hours.withIndex()) {
                val cx = padL + cellW * i + cellW / 2f
                canvas.drawText(forecast.displayTime, cx, timeBaseline, timePaint)
                canvas.drawText("${forecast.temp}°", cx, tempY, hTempPaint)
            }
        }
    }

    // ── Placeholder (translucent glass, not solid color) ──────────────

    private fun createGlassPlaceholder(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val wf = w.toFloat()
        val hf = h.toFloat()

        // Clip to round rect
        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, 0f, wf, hf), CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        })

        // Gradient background (soft, translucent glass)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, hf,
                Color.argb(45, 30, 30, 60),
                Color.argb(35, 20, 20, 50),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, wf, hf, bgPaint)

        // Glass overlay
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(4f, 4f, wf - 4f, hf - 4f), CORNER_RADIUS - 4f, CORNER_RADIUS - 4f, glassPaint)

        // Subtle highlight
        val highlight = Paint().apply {
            shader = LinearGradient(
                0f, 2f, 0f, hf * 0.3f,
                Color.argb(20, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 2f, wf, hf * 0.3f, highlight)

        // Centered hint text
        val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 255, 255, 255)
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("打开应用查看天气", wf / 2f, hf / 2f + 7f, hintPaint)

        return bitmap
    }
}