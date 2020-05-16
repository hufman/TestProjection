package me.hufman.testprojection

import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import android.view.Surface

class JPGEncoder(val width:Int = 720, val height:Int = 480, val source: Surface) {
	companion object {
		val TAG = "JPGEncoder"
		val MIMETYPE = "video/mjpeg"
	}

	val codec: MediaCodec = selectCodec(MIMETYPE)

	fun selectCodec(mimetype: String): MediaCodec {
		val codec = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { codec ->
			codec.isEncoder
		}.first { codec ->
			codec.supportedTypes.any {
				Log.i(TAG, "Found encoder for ${it.toLowerCase()}")
				it.toLowerCase() == mimetype
			}
		}.let{ codec ->
			val capabilities = codec.getCapabilitiesForType(mimetype)
			capabilities.colorFormats.forEach {
				Log.i(TAG, "Found MJPEG encoder color format ${Integer.toHexString(it)}")
			}
			MediaCodec.createByCodecName(codec.name)
		}

		val format = MediaFormat.createVideoFormat(MIMETYPE, width, height)
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888)
		format.setInteger(MediaFormat.KEY_BIT_RATE, 500)
		format.setInteger(MediaFormat.KEY_FRAME_RATE, 10)
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)
		codec.configure(format, null, null, CONFIGURE_FLAG_ENCODE)
		codec.setInputSurface(source)
		codec.start()
		return codec
	}

	fun getFrame(): ByteArray {
		val bufferInfo = MediaCodec.BufferInfo()
		while (true) {
			val encodeStatus = codec.dequeueOutputBuffer(bufferInfo, 60000000)
			when (encodeStatus) {
				MediaCodec.INFO_TRY_AGAIN_LATER -> Log.i(TAG, "Timeout encoding frame")
				MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.i(TAG, "Output format changed")
				in Int.MIN_VALUE..0 -> Log.i(TAG, "Unexpected encode index $encodeStatus")
				else -> {
					val bufferIndex = encodeStatus
					val buffer = codec.getOutputBuffer(bufferIndex)
					val array = ByteArray(bufferInfo.size)
					buffer?.get(array)
					codec.releaseOutputBuffer(bufferIndex, false)
					return array
				}
			}
		}
	}
}