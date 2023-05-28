package app.quarkton.utils

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.math.roundToInt


// Thanks to https://github.com/raheemadamboev/qr-code-scanner for great idea how to stuff those in

class QRCodeAnalyzer(
    private val onQrCodeScanned: (result: String?) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private val SUPPORTED_IMAGE_FORMATS = listOf(ImageFormat.YUV_420_888,
            ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
    }

    override fun analyze(image: ImageProxy) {
        image.use { im ->
            if (im.format in SUPPORTED_IMAGE_FORMATS) {
                val bytes = im.planes.first().buffer.toByteArray()
                val vfsize = (min(im.width, im.height) * 0.65f).roundToInt()
                val source = PlanarYUVLuminanceSource(
                    /* yuvData = */ bytes,
                    /* dataWidth = */ im.width,
                    /* dataHeight = */ im.height,
                    /* left = */ (im.width - vfsize) / 2,
                    /* top = */ (im.height - vfsize) / 2,
                    /* width = */ vfsize,
                    /* height = */ vfsize,
                    /* reverseHorizontal = */ false
                )
                nextAnalyze(source)
            }
        }
    }

    fun streamAnalyze(inputStream: InputStream, notFound: (() -> Unit)? = null) {
        try {
            var bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) {
                Log.e("QRCodeAnalyzer", "stream is not a bitmap")
                return
            }
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.recycle()
            @Suppress("UNUSED_VALUE")
            bitmap = null
            val source = RGBLuminanceSource(width, height, pixels)
            nextAnalyze(source, notFound)
        }
        catch (e: Throwable) {
            Log.e("QRCodeAnalyzer", "processing stream failed", e)
        }
    }

    fun nextAnalyze(source: LuminanceSource, notFound: (() -> Unit)? = null) {
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val binaryBitmapI = BinaryBitmap(HybridBinarizer(source.invert()))
        var nf = false
        try {
            val result = MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                        // DecodeHintType.ALSO_INVERTED to true // For Ton Connect, for example
                        // Does not work!
                    )
                )
            }.decode(binaryBitmap)
            onQrCodeScanned(result.text) //  + "\n\n${image.width}x${image.height}")
            return
        } catch (e: Exception) {
            if (e is NotFoundException)
                nf = true
            else
                Log.e("QRCodeAnalyzer", "Analysis failed", e)
        }
        try {
            val result = MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                        // DecodeHintType.ALSO_INVERTED to true // For Ton Connect, for example
                    )
                )
            }.decode(binaryBitmapI)
            onQrCodeScanned(result.text) //  + "\n\n${image.width}x${image.height}")
            return
        } catch (e: Exception) {
            if (e is NotFoundException)
                nf = true
            else
                Log.e("QRCodeAnalyzer", "Analysis failed (inverted)", e)
        }
        if (nf)
            notFound?.invoke()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        return ByteArray(remaining()).also { get(it) }
    }
}