package app.quarkton.ui.elements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TonToggle(
    value: Boolean,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null
) {
    val color = if (value) Colors.Primary else Colors.TextInactive
    val animPosition by animateFloatAsState(
        label = "togglePosition",
        targetValue = if (value) 1f else 0f,
        animationSpec = tween(300)
    )
    val animColor by animateColorAsState(
        label = "toggleColor",
        targetValue = color,
        animationSpec = tween(300)
    )
    val oc: () -> Unit = { onToggle?.invoke(!value) }
    Surface(
        color = Color.White, modifier = Modifier.height(20.dp).width(38.dp).then(modifier)
    ) {
        Surface(color = animColor, shape = RoundedCornerShape(7.dp), onClick = oc,
            modifier = Modifier.fillMaxSize().padding(2.5.dp, 2.5.dp, 3.dp, 3.dp)
                .alpha(if (disabled) 0.5f else 1f)) {}
        Row(modifier = Modifier.fillMaxSize()) {
            if (animPosition > 0)
                Spacer(modifier = Modifier.weight(animPosition))
            Box(modifier = Modifier.aspectRatio(1f)) {
                Surface(shape = CircleShape, color = animColor, modifier = Modifier.fillMaxSize()) {
                    Surface(shape = CircleShape, color = if (disabled) Colors.LightPadColor else Color.White, onClick = oc,
                        modifier = Modifier.fillMaxSize().padding(1.5.dp, 1.5.dp, 2.dp, 2.dp)) {}
                }
            }
            if (animPosition < 1)
                Spacer(modifier = Modifier.weight(1 - animPosition))
        }
    }
}

@Preview
@Composable
private fun TogglePreview() {
    QuarkTONWalletTheme {
        var value by remember { mutableStateOf(true) }

        Column(modifier = Modifier.width(40.dp)) {
            Spacer(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )

            TonToggle(value = value) {
                value = it
            }

            Spacer(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )

            TonToggle(value = !value) {
                value = !it
            }

            Spacer(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )

            TonToggle(value = true, disabled = true)

            Spacer(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )

            TonToggle(value = false, disabled = true)

            Spacer(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )
        }
    }
}