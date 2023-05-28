package app.quarkton.ui.screens.other

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.theme.Styles

class InspectorScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)
        if (!mdl.developmentMode) {
            SideEffect {
                nav?.replaceAll(StartupScreen())
            }
            return
        }
        val prefs = per.getAll()
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)) {
            Text(text = "All secure SharedPreferences entires:", color = Color.Red,
                style = Styles.smallPageTitle, modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp))
            for (kv in prefs!!) {
                Text(text = "${kv.key}: ${kv.value}", color = Color.White, style = Styles.mainText,
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(5.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    val res = per.hardReset()
                    Toast.makeText(act, "Persistence.hardReset result: $res", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth(),
                shape = Styles.buttonShape
            ) {
                Text(
                    text = "Reset secure SharedPreferences",
                    style = Styles.mainText, color = Color.Red
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

}
