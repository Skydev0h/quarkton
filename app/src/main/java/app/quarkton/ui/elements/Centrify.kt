package app.quarkton.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Centrify(
    content: @Composable BoxScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Row() {
            Spacer(Modifier.weight(1f))
            Box() {
                content()
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
    }
}
