package app.quarkton.ui.elements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.math.MathUtils.clamp
import app.quarkton.ui.theme.Colors

@Composable
fun BiathlonBox(
    count: Int, filled: Int, dark: Boolean = false, error: Boolean = false, scale: Boolean = false
) {
    // DONE: Animations - grow from center, overgrow on confirm / error
    // Handle with extra care!

    val filledColor = if (dark) Color.White else Color.Black
    val outlineColor = if (dark) Colors.BiathlonDarkOutline else Colors.TextInactive

    /*
    val infiniteTransition = rememberInfiniteTransition()

    val position by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(50),
            repeatMode = RepeatMode.Reverse
        )
    )
    */

    val anifill by animateFloatAsState(targetValue = if (scale) (count+1).toFloat() else filled.toFloat(),
        tween(if (error) 1000 else 300))
    val uber by animateFloatAsState(targetValue = if (scale) 0.5f else 0f, tween(300))

    fun e(c: Color): Color {
        return if (error) Color.Red else c
    }

    val anicolor by animateColorAsState(targetValue = e(filledColor), tween(300))

    Row(modifier = Modifier.padding(12.dp)
            // .then(if (error) Modifier.offset(position.dp, 0.dp) else Modifier)
    ) {
        for (i in 1..count) {
            Box {
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(1.dp, outlineColor),
                    color = Color.Transparent,
                    modifier = Modifier.size(16.dp)
                ) {}
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(1.dp, e(filledColor)),
                    color = anicolor,
                    modifier = Modifier
                        .size(16.dp)
                        .zIndex(1f)
                        .graphicsLayer {
                            val s = clamp(anifill - (i - 1), 0f, 1f) + uber
                            scaleX = s
                            scaleY = s
                        }
                ) {}
            }
            if (i < count) {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Row {
        for (dark in arrayOf(false, true))
            Column(modifier = Modifier.background(if (dark) Color.Black else Color.White)) {
                for (error in arrayOf(false, true)) {
                    for (i in 0..4) {
                        BiathlonBox(count = 4, filled = i, dark = dark, error = error)
                        if (i < 4)
                            Spacer(
                                modifier = Modifier
                                    .height(3.dp)
                                    .background(Color.Green)
                            )
                    }
                    Spacer(
                        modifier = Modifier
                            .height(3.dp)
                            .background(Color.Red)
                    )
                    for (i in 0..6) {
                        BiathlonBox(count = 6, filled = i, dark = dark, error = error)
                        if (i < 6)
                            Spacer(
                                modifier = Modifier
                                    .height(3.dp)
                                    .background(Color.Green)
                            )
                    }
                    Spacer(
                        modifier = Modifier
                            .height(3.dp)
                            .background(Color.Red)
                    )
                }
            }
    }
}

