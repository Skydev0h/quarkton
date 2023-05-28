package app.quarkton.ui.screens.wallet

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.breakMiddle
import app.quarkton.extensions.shortAddr
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vibrateError
import app.quarkton.extensions.vibrateKeyPress
import app.quarkton.extensions.vibrateLongPress
import app.quarkton.ui.elements.BackButton
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TextFieldPad
import app.quarkton.ui.screens.settings.CheckPasscodeScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

const val WARN_COMMENT_LENGTH = 102
const val MAX_COMMENT_LENGTH = 122

class SendCommentScreen : BaseWalletScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)
        val view = LocalView.current
        val cm = LocalClipboardManager.current
        fun copy(s: String) {
            cm.setText(AnnotatedString(s))
            Toast
                .makeText(act, "Copied to clipboard", Toast.LENGTH_SHORT)
                .show()
        }

        val wallet by db.walletDao().observeCurrent().observeAsState()
        val balance = remember(wallet?.balance) { wallet?.balance?.simpleBalance() ?: "" }

        val textFieldColors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedIndicatorColor = Colors.TextInactive,
            focusedIndicatorColor = Colors.Primary,
            disabledContainerColor = Color.White,
            disabledIndicatorColor = Colors.TextInactive
        )

        var expandRecipient by remember { mutableStateOf(false) }

        var warning by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()

        var comment by remember { mutableStateOf(TextFieldValue(mdl.sendingComment)) }

        // val fee by remember { mutableStateOf(5000000L) } // DONE: Really estimate the fee!
        // TODO: Maybe find way to *really* estimate the fee
        val fee by remember { derivedStateOf {
            (comment.text.length * 5380L + 6804000L) *
                    (if (wallet?.verRev?.let { it >= 0 } != false) 1 else 10) // MC fees x10
        } }

        fun processNewComment(t: TextFieldValue) {
            val s = t.annotatedString.toString()
            val b = s.encodeToByteArray()
            val l = b.size
            error = false
            if (l >= WARN_COMMENT_LENGTH) {
                if (l <= MAX_COMMENT_LENGTH) {
                    warning = act.getString(R.string.n_characters_left, MAX_COMMENT_LENGTH - l)
                    comment = t.copy(s)
                } else {
                    warning = act.getString(
                        R.string.message_size_has_been_exceeded_by_n_characters,
                        l - MAX_COMMENT_LENGTH
                    )
                    error = true
                    // Need to determine amount of good characters. Not too simple tasks because of
                    //     UTF-8 variable length character encoding. Need some adjustments.
                    var rba = b.take(MAX_COMMENT_LENGTH).toByteArray()
                    var good = 0
                    for (i in 0..8) {
                        try {
                            val chk = String(rba)
                            if (chk == s.substring(0, chk.length)) {
                                good = chk.length
                                break
                            }
                        } catch (_: Throwable) {}
                        rba = rba.dropLast(1).toByteArray()
                    }
                    val sb = AnnotatedString.Builder(s)
                    sb.addStyle(SpanStyle(color = Colors.TextRed, background = Colors.TextRedBack), good, s.length)
                    comment = t.copy(sb.toAnnotatedString())
                }
            } else
                comment = t.copy(s)
        }

        val confirmtx = stringResource(R.string.confirm_transaction)
        fun doSendTx() {
            dm.sendTransaction(
                mdl.sendingToAddress,
                mdl.sendingAmount,
                mdl.sendingComment,
                mdl.sendAllBalance
            )
            nav?.replaceAll(listOf(MainWalletScreen(), TransferSentScreen()))
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Surface(
                color = Color.White, shape = Styles.largePanelShapeTop,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxWidth())
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BackButton()
                            Spacer(modifier = Modifier
                                .width(20.dp)
                                .height(56.dp))
                            Text(
                                text = stringResource(R.string.send_ton),
                                style = Styles.cardLabel,
                                color = Color.Black
                            )
                        }
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 0.dp)) {
                                Text(
                                    text = stringResource(R.string.comment_optional),
                                    style = Styles.primlabel, color = Colors.Primary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                TextFieldPad(
                                    value = comment, onValueChange = { processNewComment(it) },
                                    colors = textFieldColors,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(0.dp, 12.dp),
                                    minHeight = 44.dp,
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.description_of_the_payment),
                                            style = Styles.mainText, color = Colors.Gray
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.the_comment_is),
                                    style = Styles.smallHint, color = Colors.Gray
                                )
                                if (warning != "") {
                                    Text(
                                        text = warning,
                                        style = Styles.smallHint, color =
                                            if (error) Colors.TextRed else Colors.TextOrange
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            UniversalItem(header = stringResource(id = R.string.details))
                            UniversalItem(text = stringResource(R.string.recipient),
                                valueColor = Color.Black,
                                valueStyle = if (expandRecipient) Styles.smallAddress else Styles.address,
                                value = if (expandRecipient) mdl.sendingToAddress.breakMiddle() else
                                    mdl.sendingToAddress.shortAddr(),
                                onLongClick = {
                                    view.vibrateLongPress()
                                    copy(mdl.sendingToAddress)
                                }
                            ) {
                                expandRecipient = !expandRecipient
                            }
                            UniversalItem(text = stringResource(R.string.amount),
                                valueColor = Color.Black, gemIcon = true,
                                value = (if (mdl.sendAllBalance) balance else mdl.sendingAmount.simpleBalance(9))
                                        + (if (mdl.sendAllBalance) " (all)" else ""),
                                disableRipple = true
                            )
                            UniversalItem(text = stringResource(R.string.fee),
                                valueColor = Color.Black, gemIcon = true,
                                value = "≈ " + fee.simpleBalance(3),
                                last = true,
                                disableRipple = true
                            )
                        }
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 16.dp, 16.dp, 16.dp)) {
                            // *************************************************************************
                            Button(
                                onClick = {
                                    if (error) {
                                        view.vibrateError()
                                        return@Button
                                    }
                                    mdl.sendingComment = comment.text
                                    // Replace screens with correct chain: Main wallet -> Sending TON
                                    if (per.reqAuthForSend) {
                                        mdl.nextSettingsAllowFP = true
                                        mdl.nextSettingsAction = { doSendTx() }
                                        mdl.nextSettingsTitle.value = confirmtx
                                        nav?.push(CheckPasscodeScreen())
                                    } else
                                        doSendTx()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Styles.buttonHeight),
                                shape = Styles.buttonShape
                            ) {
                                Text(
                                    text = stringResource(R.string.confirm_and_send),
                                    style = Styles.buttonLabel,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // *************************************************************************
                        }
                    }
                }
            }
        }
    }
}