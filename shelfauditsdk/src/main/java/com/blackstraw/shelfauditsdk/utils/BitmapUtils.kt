package com.blackstraw.shelfauditsdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.blackstraw.shelfauditsdk.ml.BoundingBox

/**
 * Function to get a Bitmap from the assets folder.
 * The function will take a context and a file path as input.
 * The function will open the file from the assets folder and decode it into a Bitmap.
 *
 * @param context The context to access the assets.
 * @param filePath The file path in the assets folder.
 * @return The decoded Bitmap.
 */
fun getBitmapFromAssets(context: Context, filePath: String): Bitmap {
    val assetManager = context.assets
    val istr = assetManager.open(filePath)
    return BitmapFactory.decodeStream(istr)
}

fun drawBoundingBoxesOnBitmap(
    bitmap: Bitmap,
    boundingBoxes: List<BoundingBox>
): Bitmap {
    try {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        for (box in boundingBoxes) {
            val paint = Paint().apply {
                color = if (box.clsName.contains("PRODUCT")) Color.RED else Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            // Scale coordinates if they are normalized
            val x1 = box.x1 * bitmapWidth
            val y1 = box.y1 * bitmapHeight
            val x2 = box.x2 * bitmapWidth
            val y2 = box.y2 * bitmapHeight

            // Draw only if the box is within bounds
            if (x1 >= 0 && y1 >= 0 && x2 <= bitmapWidth && y2 <= bitmapHeight) {
                canvas.drawRect(x1, y1, x2, y2, paint)
                //   canvas.drawText(box.clsName, x1, y1 - 10, textPaint)
            }
        }

        return mutableBitmap
    }catch (exception : Exception){
        Log.e("Crash", "drawBoundingBoxesOnBitmap: exception - ${exception.message}", )
        return  Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }

}

/*
* return simple Bitmap which is square bounding box of 100.dp
* */
fun getSimpleBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawRect(0f, 0f, 100f, 100f, paint)
    return bitmap
}