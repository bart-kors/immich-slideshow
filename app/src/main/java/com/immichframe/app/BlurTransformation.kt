package com.immichframe.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil.size.Size
import coil.transform.Transformation

/**
 * Copied (and trimmed) from Coil 2.0's removed BlurTransformation.
 * RenderScript-based; safe on every Android version our minSdk (23) supports.
 */
@Suppress("DEPRECATION")
class BlurTransformation(
    private val context: Context,
    private val radius: Float = 25f,
    private val sampling: Float = 6f,
) : Transformation {

    init {
        require(radius in 0.0..25.0) { "radius must be in [0, 25]." }
        require(sampling > 0) { "sampling must be > 0." }
    }

    override val cacheKey: String = "BlurTransformation-$radius-$sampling"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val scaledWidth = (input.width / sampling).toInt().coerceAtLeast(1)
        val scaledHeight = (input.height / sampling).toInt().coerceAtLeast(1)

        val config = input.config ?: Bitmap.Config.ARGB_8888
        val output = createBitmap(scaledWidth, scaledHeight, config)

        Canvas(output).apply {
            scale(1f / sampling, 1f / sampling)
            drawBitmap(input, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }

        var rs: RenderScript? = null
        var inAlloc: Allocation? = null
        var outAlloc: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            rs = RenderScript.create(context)
            inAlloc = Allocation.createFromBitmap(rs, output)
            outAlloc = Allocation.createTyped(rs, inAlloc.type)
            blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            blur.setRadius(radius.coerceIn(0f, 25f))
            blur.setInput(inAlloc)
            blur.forEach(outAlloc)
            outAlloc.copyTo(output)
        } finally {
            inAlloc?.destroy()
            outAlloc?.destroy()
            blur?.destroy()
            rs?.destroy()
        }

        return output
    }

    override fun equals(other: Any?) = other is BlurTransformation &&
        radius == other.radius && sampling == other.sampling

    override fun hashCode(): Int = radius.hashCode() * 31 + sampling.hashCode()
}
