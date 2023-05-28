@file:OptIn(ExperimentalAnimationApi::class)

package app.quarkton.ui

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Up
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Right
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import app.quarkton.ui.screens.other.*
import app.quarkton.ui.screens.onboarding.*
import app.quarkton.ui.screens.settings.SettingsScreen
import app.quarkton.ui.screens.wallet.*
import cafe.adriel.voyager.core.stack.StackEvent.Pop
import kotlin.math.roundToInt

@Composable
fun MyScreenTransition(
    navigator: Navigator, modifier: Modifier = Modifier, content: ScreenTransitionContent
) {
    ScreenTransition(navigator = navigator, modifier = modifier, content = content, transition = {
        Log.i("Transition", this.initialState.javaClass.simpleName + " to " + this.targetState.javaClass.simpleName)
        val f = this.initialState
        val t = this.targetState
        val e = navigator.lastEvent
        if ((t is WelcomeScreen && f is StartupScreen) ||
            (t is LockScreen))
            return@ScreenTransition fade()
        if (t is MainWalletScreen) {
            if (f is SettingsScreen) return@ScreenTransition scale(sX = 0f, sY = 0f, tX = 1f, tY = 0f)
            if (f is QRScanScreen) return@ScreenTransition scale(tX = 0.8f, tY = 0f)
            return@ScreenTransition fadeslide(if (f is LockScreen) Up else Down)
        }
        if (e == Pop)
            return@ScreenTransition fadeslide(Right)
        when (t) {
            is WelcomeScreen -> fade()
            is SettingsScreen -> scalefade(sX = 1f, sY = 0f, tX = 0f, tY = 0f)
            is SendAddressScreen -> slide(Up)
            is QRScanScreen -> scalefade(sX = 0.8f, sY = 0f)
            // is TonConnectScreen -> scalefade(sX = 0.7f, sY = 0f)
            else -> fadeslide(Left)
        }
    })
}

private fun fade(duration: Int = 300, multiplier: Float = 2f,
                   fadeInAlpha: Float = 0f, fadeOutAlpha: Float = 0f): ContentTransform {
    return fadeIn(tween(duration), fadeInAlpha) with
           fadeOut(tween((duration * multiplier).roundToInt()), fadeOutAlpha)
}

private fun scale(duration: Int = 300, multiplier: Float = 2f,
                  sX: Float = 0.5f, sY: Float = 0.5f,
                  tX: Float = 0.5f, tY: Float = 0.5f): ContentTransform {
    return scaleIn(tween(duration), transformOrigin = TransformOrigin(sX, sY)) with
           scaleOut(tween((duration * multiplier).roundToInt()), transformOrigin = TransformOrigin(tX, tY))
}

private fun scalefade(duration: Int = 300, multiplier: Float = 2f,
                  sX: Float = 0.5f, sY: Float = 0.5f,
                  tX: Float = 0.5f, tY: Float = 0.5f,
                  fadeDuration: Int = 300, fadeMultiplier: Float = 2f,
                  fadeInAlpha: Float = 0.3f, fadeOutAlpha: Float = 0.3f): ContentTransform {
    return scaleIn(tween(duration), transformOrigin = TransformOrigin(sX, sY)) +
            fadeIn(tween(fadeDuration), fadeInAlpha) with
           scaleOut(tween((duration * multiplier).roundToInt()), transformOrigin = TransformOrigin(tX, tY)) +
            fadeOut(tween((fadeDuration * fadeMultiplier).roundToInt()), fadeOutAlpha)
}

private fun<S> AnimatedContentScope<S>.slide(direction: AnimatedContentScope.SlideDirection,
                    duration: Int = 300, multiplier: Float = 2f): ContentTransform {
    return slideIntoContainer(direction, tween(duration)) with
           slideOutOfContainer(direction, tween((duration * multiplier).roundToInt()))
}

private fun<S> AnimatedContentScope<S>.fadeslide(direction: AnimatedContentScope.SlideDirection,
                                                     fadeDuration: Int = 300, fadeMultiplier: Float = 2f,
                                                     slideDuration: Int = 300, slideMultiplier: Float = 2f,
                                                     fadeInAlpha: Float = 0.3f, fadeOutAlpha: Float = 0.3f): ContentTransform {
    return slideIntoContainer(direction, tween(slideDuration)) +
            fadeIn(tween(fadeDuration), fadeInAlpha) with
           slideOutOfContainer(direction, tween((slideDuration * slideMultiplier).roundToInt())) +
            fadeOut(tween((fadeDuration * fadeMultiplier).roundToInt()), fadeOutAlpha)
}