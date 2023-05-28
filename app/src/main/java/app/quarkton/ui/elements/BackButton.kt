package app.quarkton.ui.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.LocalNavigator

@Preview
@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    onClick: (() -> Unit)? = null
) {
    val nav = LocalNavigator.current
    IconButton(onClick = { if (onClick != null) onClick() else nav?.pop() }, modifier = modifier) {
        Icon(
            tint = color,
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}