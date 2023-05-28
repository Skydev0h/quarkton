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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.simpleBalance
import app.quarkton.ton.DataMaster
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

class CurrencyListScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)

        val scrollState = rememberScrollState()

        val wallet = db.walletDao().observeCurrent().observeAsState()
        val rates = db.rateDao().observeAll().observeAsState()
        var mycurr by remember { mutableStateOf(per.selectedCurrency) }

        TopBar(color = Color.Black, textColor = Color.White, backColor = Color.White,
            titleText = stringResource(R.string.select_primary_currency), backIcon = true)

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
                        val rv = rates.value ?: listOf()
                        rv.forEach {
                            val sel = (mycurr == it.name)
                            UniversalItem(
                                text = it.name,
                                subText = DataMaster.currencies[it.name]?.second,
                                color = if (sel) Colors.Primary else Color.Black,
                                valueColor = if (sel) Colors.Primary else Color.Gray,
                                subColor = if (sel) Color.Black else Color.Gray,
                                style = if (sel) Styles.primlabel else Styles.mainText,
                                valueStyle = if (sel) Styles.primlabel else Styles.mainText,
                                value =
                                DataMaster.currencies[it.name]?.first?.replace(
                                    "~",
                                    String.format(
                                        "%.2f",
                                        (wallet.value?.balance?.simpleBalance(9)?.toDouble()
                                            ?: 0.0) * it.rate
                                    )
                                ) ?: ""
                            ) { _ ->
                                per.selectedCurrency = it.name
                                mycurr = it.name
                            }
                        }
                    }
                }
            }
        }
    }

}