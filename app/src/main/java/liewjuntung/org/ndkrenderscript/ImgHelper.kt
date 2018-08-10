package liewjuntung.org.ndkrenderscript

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.os.Build
import android.renderscript.*
import android.util.Log
import java.io.*
import java.nio.ByteBuffer

class ImageHelper(val context: Context) {
    val TAG = ImageHelper::class.java.simpleName


}

fun Bitmap.rotateImage(angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix,
            true)
}