package me.hufman.testprojection

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.ImageReader

object Data {
	val rect = Rect(0, 0, 720, 480)
	val imageCapture = ImageReader.newInstance(rect.width(), rect.height(), ImageFormat.PRIVATE, 2)!!
	val encoder = JPGEncoder(imageCapture.width, imageCapture.height, imageCapture.surface)
	var bitmap = Bitmap.createBitmap(imageCapture.width, imageCapture.height,  Bitmap.Config.ARGB_8888)!!
}