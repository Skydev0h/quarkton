package app.quarkton.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quarkton.R

@Suppress("MemberVisibilityCanBePrivate")
class Styles {

    companion object {

        val buttonShape = RoundedCornerShape(8.dp)
        val smallButtonShape = RoundedCornerShape(6.dp)

        val smallPanelShapeTop = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
        val smallPanel = RoundedCornerShape(6.dp)

        val panelShapeTop = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
        val panelShape = RoundedCornerShape(10.dp)

        val largePanelShapeTop = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        val largePanelShape = RoundedCornerShape(14.dp)

        val commentShape = RoundedCornerShape(4.dp, 10.dp, 10.dp, 10.dp)

        val buttonHeight = 48.dp
        val smallButtonHeight = 36.dp

        // @ 24, 28 @  Wizard title, page titles
        val pageTitle = font(24, 28, true)
        val wizardTitle = pageTitle

        // @ 20, 24 @  Card labels, small page titles
        val cardLabel = font(20, 24, true)
        val smallPageTitle = cardLabel

        val cardLabelThin = font(20, 24)

        // @ 19, 26 @  Modal title
        val modalTitle = font(19, 26, true)

        // [ 16, 20 ]  Passcode length list entries
        val passcodeListEntry = font(16, 20)
        val recentsEntry = passcodeListEntry

        val tonConnect = font(16, 20, true)

        // [ 15, 20 ]  Wizard text, modal text, descriptions, texts...
        val mainText = font(15, 20)

        // [ 15, 16 ]  Small table header
        val smallHeader = font(15, 16, true)

        // @ 15,,20 @  Button labels
        val buttonLabel = font(15, 20, true, spacing = 0.1)

        // [ 15,,20 ]  Text button labels
        val textButtonLabel = font(15, 20, false, spacing = 0.1)

        // [ 14, 18 ]  Modal box labels, check box labels
        val checkBox = font(14, 18)
        val txListText = checkBox
        val smallButtonText = checkBox
        val popupBoxText = checkBox

        // @ 14, 18 @  Modal box labels, check box labels
        val modalButton = font(14, 18, true)
        val popupBoxTitle = modalButton

        // [ 13, 16 ]  Small hint texts
        val smallHint = font(13, 16)

        val smallCategory = font(13, 16, true)

        // > 15, 20 <  Addresses
        val address = font(15, 20, mono = true)

        val smallAddress = font(13, 16, mono = true)

        val txAddress = font(14, 18, mono = true)

        // @ 15, 20 @  Button labels
        val phrase = font(15, 20, true)
        val date = phrase
        val primlabel = phrase

        // [ 24, 28 ]  Keypad numbers
        val keyPadNumber = font(24, 28)

        // [ 14, 22 ]  Keypad letter hints
        val keyPadHints = font(14, 22)

        val topBalanceText = font(18, 24, true)
        val topBalanceSubText = font(14, 16)
        
        val bigBalanceText = font(44, 56, true).copy(
            fontFamily = FontFamily(Font(R.font.google_sans_medium))
        )
        val bigBalanceDeciSize = 32.sp
        val txAmtDeciSize = 14.sp
        // val bigBalancePart = font(32, 40, true)

        val paymentListText = font(14, 20)

        val txAmtSmall = font(18, 20, true)

        val shortAddress = font(15, 18)
        val transSubText = shortAddress
        val recentsSubText = shortAddress

        val pullRefresh = font(15, 18, true)
        

        private fun font(fontSize: Int, lineHeight: Int, bold: Boolean = false,
                         spacing: Double = 0.0, mono: Boolean = false): TextStyle {
            return TextStyle(
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
                fontSize = fontSize.sp,
                lineHeight = lineHeight.sp,
                letterSpacing = spacing.sp
            )
        }

        // Set of Material typography styles to start with
        val Typography = Typography(
            titleLarge = wizardTitle,
            titleMedium = cardLabel,
            titleSmall = modalTitle,
            bodyLarge = passcodeListEntry,
            bodyMedium = mainText,
            bodySmall = smallHeader,
            labelLarge = buttonLabel,
            labelMedium = checkBox,
            labelSmall = smallHint,
            displaySmall = address,
            displayMedium = textButtonLabel
        )

    }

}