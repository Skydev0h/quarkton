package app.quarkton.ui.elements

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import org.ton.mnemonic.Mnemonic
import java.lang.Exception

@Composable
fun SeedTextField(
    index: Int,
    numbers: Array<Int>,
    texts: Array<MutableState<TextFieldValue>>,
    focusreqs: Array<FocusRequester>? = null,
    onWordComplete: ((Int) -> Unit)? = null,
    padBottom: Int = 8,
    autoNext: Boolean = true,
    hidePopup: Boolean = false,
    keyboardActions: KeyboardActions? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val number = remember { numbers[index] }
    val text = remember { texts[index] }
    val lastText = remember { mutableStateOf(texts[index].value.text) }
    val words = remember { Mnemonic.mnemonicWords() }
    val popupHeight = remember { mutableStateOf(0) }
    val focused = remember { mutableStateOf(false) }
    val focusRequester = remember { if (focusreqs != null) focusreqs[index] else FocusRequester() }
    val focusManager = LocalFocusManager.current
    val popupState = rememberLazyListState()
    val textFieldColors = TextFieldDefaults.colors(
        unfocusedContainerColor = Color.White,
        focusedContainerColor = Color.White,
        unfocusedIndicatorColor = Colors.TextInactive,
        focusedIndicatorColor = Colors.Primary
    )
    val hintItems = remember { mutableStateListOf<String>() }
    val density = LocalDensity.current
    val offsetLeft = remember { with(density) { 5.dp.toPx().toInt() } }
    fun wordCompleted() {
        hintItems.clear()
        onWordComplete?.invoke(index)
        if (autoNext && focusreqs != null) {
            coroutineScope.launch {
                if (index + 1 < focusreqs.size) {
                    try {
                        focusreqs[index + 1].requestFocus()
                    } catch (e: Exception) {
                        Log.w("SeedTextField", "Failed to move focus")
                        focusManager.clearFocus()
                    }
                } else {
                    focusManager.clearFocus()
                }
            }
        }
    }
    Box(modifier = Modifier.padding(0.dp, 0.dp, 0.dp, padBottom.dp)) {
        TextFieldPad(value = text.value, onValueChange = { tfv ->
            text.value = tfv
            if (text.value.text != lastText.value) { // React only to ACTUAL text changes
                lastText.value = text.value.text
                hintItems.clear()
                if (tfv.text != "") {
                    hintItems.addAll(words.filter { it.startsWith(tfv.text) })
                    if (hintItems.size == 1 && hintItems[0] == tfv.text) {
                        wordCompleted()
                    }
                    coroutineScope.launch {
                        popupState.scrollToItem(0)
                    }
                }
            }
        }, colors = textFieldColors,
            textStyle = Styles.mainText,
            modifier = Modifier
                .width(200.dp)
                .onFocusChanged { focused.value = it.isFocused }
                .focusRequester(focusRequester),
            singleLine = true, contentPadding = PaddingValues(0.dp, 12.dp),
            minHeight = 44.dp,
            prefix = {
                Row {
                    Text(
                        text = "${number}:", modifier = Modifier.width(24.dp),
                        color = Colors.Gray, style = Styles.mainText,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.width(4.dp)) // About 2.dp internal padding
                }
            },
            // Let keyboard know that this is password-like input!
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            keyboardActions = keyboardActions ?: KeyboardActions()
        )
        if (focused.value && hintItems.size > 0 && !hidePopup) {
            Popup(alignment = Alignment.TopStart, offset = IntOffset(-offsetLeft, -popupHeight.value)) {
                Row(modifier = Modifier.onSizeChanged {
                    popupHeight.value = it.height
                }) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Surface(
                        shadowElevation = 2.dp,
                        modifier = Modifier,
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        LazyRow(
                            modifier = Modifier.widthIn(0.dp, 200.dp),
                            state = popupState
                        ) {
                            item { Spacer(modifier = Modifier.width(5.dp)) }
                            items(hintItems, itemContent = {
                                TextButton(
                                    onClick = {
                                        text.value = text.value.copy(it, TextRange(it.length))
                                        hintItems.clear()
                                        wordCompleted()
                                    },
                                    contentPadding = PaddingValues(16.dp, 12.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = it,
                                        style = Styles.mainText,
                                        color = Color.Black
                                    )
                                }
                            })
                            item { Spacer(modifier = Modifier.width(5.dp)) }
                        }
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
        }
        LaunchedEffect(hidePopup, text.value) {
            if (hidePopup && hintItems.size > 0 && text.value.text == "") {
                hintItems.clear()
                lastText.value = ""
            }
        }
    }
}