package app.quarkton.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.ton.makeExplorerLink
import app.quarkton.ton.supportedExplorers
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

class ExplorersListScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)

        val scrollState = rememberScrollState()

        val allexpl = supportedExplorers()
        var myexpl by remember { mutableStateOf(per.selectedExplorer) }

        TopBar(color = Color.Black, textColor = Color.White, backColor = Color.White,
            titleText = stringResource(R.string.select_preferred_explorer), backIcon = true)

        Surface(
            modifier = Modifier.fillMaxSize(), color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize())
            {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = Styles.panelShapeTop
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                    {
                        allexpl.forEach {
                            val sel = (it.key == myexpl)
                            UniversalItem(
                                text = it.value,
                                subText = makeExplorerLink(explorer = it.key),
                                color = if (sel) Colors.Primary else Color.Black,
                                subColor = if (sel) Color.Black else Color.Gray,
                                style = if (sel) Styles.primlabel else Styles.mainText,
                            ) { _ ->
                                per.selectedExplorer = it.key
                                myexpl = it.key
                            }
                        }
                    }
                }
            }
        }
    }

}