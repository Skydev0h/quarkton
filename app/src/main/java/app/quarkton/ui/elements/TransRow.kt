package app.quarkton.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.db.TransItem
import app.quarkton.db.createMockTransaction
import app.quarkton.extensions.formatBalance
import app.quarkton.extensions.simpleBalance
import app.quarkton.ton.extensions.ZERO_TX
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransRow(
    prev: TransItem?, tx: TransItem,
    dateFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    dateFmtXL: DateTimeFormatter,
    onClick: ((TransItem, Boolean, String, Long, String?) -> Unit)? = null
) {
    val prevInst = remember { Instant.ofEpochSecond(prev?.now ?: 0) }
    val prevDate = remember {
        if (prevInst.atZone(ZoneId.systemDefault()).year != Instant.now().atZone(ZoneId.systemDefault()).year)
            dateFmtXL.format(prevInst) else dateFmt.format(prevInst)
    }
    val thisInst = remember { Instant.ofEpochSecond(tx.now) }
    val thisDate = remember {
        if (thisInst.atZone(ZoneId.systemDefault()).year != Instant.now().atZone(ZoneId.systemDefault()).year)
            dateFmtXL.format(thisInst) else dateFmt.format(thisInst)
    }
    val time = remember { timeFmt.format(thisInst) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (thisDate != prevDate) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = thisDate, style = Styles.date,
                    modifier = Modifier.padding(16.dp, 20.dp, 16.dp, 0.dp)
                )
            }
        }
        InnerTransRow(tx, time, false, tx.dst3, tx.amt3, tx.cmt3, onClick)
        InnerTransRow(tx, time, false, tx.dst2, tx.amt2, tx.cmt2, onClick)
        InnerTransRow(tx, time, false, tx.dst1, tx.amt1, tx.cmt1, onClick)
        InnerTransRow(tx, time, false, tx.dst,  tx.amt,  tx.cmt,  onClick)
        InnerTransRow(tx, time, true, tx.src, tx.inamt, tx.incmt, onClick)
    }
}

@Composable
fun InnerTransRow(
    tx: TransItem, time: String, isIncoming: Boolean,
    address: String?, amount: Long?, comment: String?,
    onClick: ((TransItem, Boolean, String, Long, String?) -> Unit)? = null
) {
    if ((address == null) || (amount == null)) return
    val failed = remember { !tx.actOk or !tx.compOk }
    fun cm(c: Color) =
        when {
            failed -> Colors.TextRed
            tx.id == ZERO_TX -> Colors.Primary
            else -> c
        }
    Surface(
        color = Color.White, modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp, 0.dp),
        onClick = {
            onClick?.invoke(tx, isIncoming, address, amount, comment)
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(text = time, style = Styles.txListText, color = cm(Colors.Gray),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(0.dp, 17.dp, 12.dp, 0.dp))
            Column(modifier = Modifier
                .fillMaxWidth().padding(12.dp, 14.dp, 12.dp, 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.gem), contentDescription = "TON",
                        modifier = Modifier.size(18.dp))
                    Text(text = amount.formatBalance(4, Styles.txAmtDeciSize),
                        color = if (isIncoming) Colors.BalGreen else Colors.BalRed,
                        style = Styles.txAmtSmall, modifier = Modifier.padding(3.dp, 0.dp))
                    Text(text = stringResource(if (isIncoming) R.string.tx_from else R.string.tx_to),
                        style = Styles.paymentListText, color = Colors.Gray,
                        modifier = Modifier.offset(0.dp, 1.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = address.substring(0, 6) + "…" + address.substring(address.length - 7),
                    color = cm(Color.Black), style = Styles.txAddress)
                if (tx.storFee != 0L) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(R.string.minus_storage_fee, tx.storFee.simpleBalance(9)),
                        color = cm(Colors.Gray), style = Styles.txListText)
                }
                if (comment != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Colors.BackGray, shape = Styles.commentShape
                    ) {
                        Text(text = comment, style = Styles.mainText, color = cm(Color.Black),
                            modifier = Modifier.padding(12.dp, 10.dp))
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Colors.TranBorder))
}

@Preview
@Composable
private fun Preview() {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMMM d")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault()) }
    val dateFmtXL = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault()) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault()) }
    val prevtx = createMockTransaction(1613081906)
    val tx = createMockTransaction(1613181906)
    // val ftx = createMockTransaction(1613191906, false)
    QuarkTONWalletTheme {
        Column {
            TransRow(prev = prevtx, tx = tx, dateFmt, timeFmt, dateFmtXL)
        }
    }
}

@Preview
@Composable
private fun PreviewFailed() {
    val dateFmt = remember {
        DateTimeFormatter.ofPattern("MMMM d")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val dateFmtXL = remember {
        DateTimeFormatter.ofPattern("MMMM d, yyyy")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val timeFmt = remember {
        DateTimeFormatter.ofPattern("HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val prevtx = createMockTransaction(1613081906)
    val tx = createMockTransaction(1613181906, false, false)
    QuarkTONWalletTheme {
        Column {
            TransRow(prev = prevtx, tx = tx, dateFmt, timeFmt, dateFmtXL)
        }
    }
}