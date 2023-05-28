package app.quarkton.ui.elements

import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import app.quarkton.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import java.io.File
import java.util.zip.GZIPInputStream

@Composable
fun Lottie(
    // Fallback is not needed anymore because a native non-JNI library was adapted for this task
    // @DrawableRes imageId: Int,
    @RawRes lottieId: Int, modifier: Modifier = Modifier,
    iterations: Int = 1, start: Float = 0f, end: Float = 1f
) {
    val ctx = LocalContext.current
    val name = ctx.resources.getResourceEntryName(lottieId)
    val desc = name.split('_').joinToString { it.replaceFirstChar(Char::uppercaseChar) }
    Box(modifier = modifier) {
        if (!LocalInspectionMode.current) {
            val json = remember(lottieId) {
                try {
                    val cacheDir = ctx.cacheDir
                    val cacheFile = File(cacheDir, "$name.tgs.json")
                    if (cacheFile.exists()) {
                        Log.i("Lottie", "Using cached $name.tgs.json")
                        return@remember cacheFile.readText()
                    }
                    Log.i("Lottie", "Unpacking $name.tgs")
                    val json = GZIPInputStream(ctx.resources.openRawResource(lottieId))
                        .bufferedReader().use { it.readText() }
                    cacheFile.writeText(json)
                    return@remember json
                } catch (_: Throwable) { }
                return@remember ""
            }
            val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(json), cacheKey = "$name.tgs")
            key(name) {
                LottieAnimation(composition, iterations = iterations, clipSpec = LottieClipSpec.Progress(start, end))
            }
        } else {
            // Only for preview in Android Studio
            Image(
                painter = painterResource(id = R.drawable.gem),
                contentDescription = desc,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}