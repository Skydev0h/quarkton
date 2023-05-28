package app.quarkton.ui.screens.wallet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.FloatingActionButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.quarkton.R
import app.quarkton.ui.elements.BackButton
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import app.quarkton.utils.QRCodeAnalyzer
import app.quarkton.utils.processDeepLink
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ton.block.AddrStd
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.camera.core.Preview as Camera_Preview

class QRScanScreen : BaseWalletScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    fun processUri(u: Uri, nav: Navigator?) {
        processDeepLink(u,
            onTonResult = { addr, amount, comment ->
                mdl.sendReset()
                mdl.sendingToAddress = addr
                if (mdl.qrFromAddrScr)
                    nav?.pop()
                else {
                    mdl.sendingAmount = amount
                    mdl.sendingComment = comment
                    nav?.replace(SendAddressScreen())
                }
            },
            onTCResult = { id, request ->
                if (mdl.qrFromAddrScr) {
                    Toast.makeText(act, act.resources.getString(
                        R.string.use_scanner_in_wallet), Toast.LENGTH_SHORT).show()
                } else {
                    mdl.tcPendingCR.value = Pair(id, request)
                    nav?.replaceAll(MainWalletScreen())
                }
            }
        )
    }

    // TODO: Investigate abysmal performance on Galaxy Note 3 Snapdragon
    // It should be some strange bug and not a performance issue because my ancient CardOCR
    //     project that does not use transparency over viewfinder works perfectly and smoothly

    @Composable
    override fun Content() {
        Init(dark = true)

        val act = LocalContext.current as FragmentActivity

        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(act) }
        var myCamera by remember { mutableStateOf<Camera?>(null) }
        var torchMode by remember { mutableStateOf(false) }

        val scanResult = remember { mutableStateOf("") }

        LifecycleEffect(onDisposed = {
            try {
                myCamera = null
                cameraProviderFuture.get().unbindAll()
            } catch (_: Throwable) {}
        })

        var hasCameraPermission by remember { mutableStateOf(false) }

        DisposableEffect(true) {
            val obs = LifecycleEventObserver {
                _, event -> if (event == Lifecycle.Event.ON_PAUSE) torchMode = false
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasCameraPermission = ContextCompat.checkSelfPermission(act,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                }
            }
            act.lifecycle.addObserver(obs)
            onDispose { act.lifecycle.removeObserver(obs) }
        }

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
            { granted -> hasCameraPermission = granted }

        LaunchedEffect(scanResult.value) {
            val sr = scanResult.value
            Log.i("QRScanScreen", "Scan result $sr")
            if (sr.startsWith("ton://") or sr.startsWith("tc://")) {
                // ton:// deep link
                try {
                    val u = Uri.parse(sr)
                    processUri(u, nav)
                } catch (e: Throwable) {
                    Log.e("QRScanner", "URI Processing failed", e)
                    Toast.makeText(act, if (e is IllegalArgumentException) e.message else
                        act.resources.getString(R.string.qr_code_seems_to_be_corrupted),
                        Toast.LENGTH_SHORT).show()
                }
            }
            else if (sr.length == 48) {
                // Maybe just an address in text format
                try {
                    AddrStd(sr) // Will throw if invalid address
                    processUri(Uri.parse("ton://transfer/$sr"), nav) // retro fit into fun
                } catch (_: Throwable) {
                    Toast.makeText(act, act.resources.getString(
                        R.string.this_qr_code_is_not_supported), Toast.LENGTH_SHORT).show()
                }
            }
            else if (scanResult.value != "" && !scanResult.value.all { it.isDigit() }) {
                // Sometimes sporadically it detects codes consisting purely of numbers
                Toast.makeText(act, act.resources.getString(
                    R.string.this_qr_code_is_not_supported), Toast.LENGTH_SHORT).show()
            }
            if (scanResult.value != "")
                crs?.launch {
                    if (!scanResult.value.all { it.isDigit() })
                        delay(1000)
                    else
                        delay(10)
                    scanResult.value = ""
                }
        }

        val analyzer = remember {
            QRCodeAnalyzer { result ->
                if (result == null) return@QRCodeAnalyzer
                scanResult.value = result
            }
        }

        val pickFileLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            try {
                if (uri != null) {
                    val ins = act.contentResolver.openInputStream(uri)
                    ins.use {
                        if (it != null) {
                            analyzer.streamAnalyze(it) {
                                Toast.makeText(
                                    act, act.resources.getString(
                                        R.string.qr_code_not_found_in_image
                                    ), Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("QRScanScreen", "Scan from file failed", e)
                Toast.makeText(act, act.resources.getString(
                    R.string.scanning_from_file_failed), Toast.LENGTH_SHORT).show()
            }
        }

        val ld = LocalDensity.current

        LaunchedEffect(true) { launcher.launch(Manifest.permission.CAMERA) }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val ar = remember(maxWidth, maxHeight) { maxWidth / maxHeight }
            // periscope size
            val peri = remember(maxWidth, maxHeight) {
                with(ld) { min(maxHeight.toPx(), maxWidth.toPx()) * 0.7f }
            }
            // half of periscope size
            val hperi = remember(peri) { peri / 2 }
            // center point
            val cent = remember(maxWidth, maxHeight) {
                with (ld) { Offset(maxWidth.toPx() / 2, maxHeight.toPx() / 2) }
            }
            // corner line size
            val csz = remember(peri) { peri / 10 }
            // corner radius size
            val rsz = remember { with(ld) { 6.dp.toPx() } }
            // corner line width
            val lwi = remember { with(ld) { 3.5.dp.toPx() } }
            Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                    BackButton(color = Color.White, modifier = Modifier
                        .padding(4.dp)
                        .size(48.dp)
                        .zIndex(3f))
                    if (hasCameraPermission) {
                        Column(modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f)) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = stringResource(id = R.string.scan_qr_code),
                                color = Color.White, style = Styles.pageTitle,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.weight(3f))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.weight(1f))
                                FloatingActionButton(onClick = {
                                    try {
                                        pickFileLauncher.launch("image/*")
                                    } catch (_: Throwable) {
                                        error("Failed to open pickFileLauncher")
                                    }
                                }, backgroundColor = Colors.QRButtonBack, contentColor = Color.White) {
                                    Icon(painter = painterResource(id = R.drawable.ic_gallery),
                                        contentDescription = "Gallery", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.weight(0.8f))
                                FloatingActionButton(onClick = {
                                    try {
                                        val c = myCamera ?: return@FloatingActionButton
                                        if (c.cameraInfo.hasFlashUnit()) {
                                            c.cameraControl.enableTorch(!torchMode)
                                            torchMode = !torchMode
                                        }
                                    } catch (_: Throwable) {}
                                }, backgroundColor = if (torchMode) Colors.QRButtonBackAct else Colors.QRButtonBack,
                                    contentColor = Color.White) {
                                    Icon(painter = painterResource(id = R.drawable.ic_flashlight),
                                        contentDescription = "Torch", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        AndroidView(factory = { context ->
                            val previewView = PreviewView(context)
                            val preview = Camera_Preview.Builder().build()
                            val selector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetResolution(
                                    android.util.Size(
                                        (1200 * ar).roundToInt(),
                                        1200
                                    )
                                )
                                .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                analyzer
                            )
                            val usecases = UseCaseGroup.Builder()
                                .addUseCase(preview).addUseCase(imageAnalysis).build()
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(act, selector, usecases)
                                myCamera = camera
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return@AndroidView previewView

                        }, modifier = Modifier.fillMaxSize())
                        Surface(color = Colors.Transparent, modifier = Modifier.fillMaxSize()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = .9999f }
                            ) {
                                drawRect(color = Color.Black.copy(alpha = 0.5f))
                                drawRoundRect(
                                    color = Color.Transparent,
                                    blendMode = BlendMode.Clear,
                                    topLeft = center - Offset(peri / 2, peri / 2),
                                    size = Size(peri, peri),
                                    cornerRadius = CornerRadius(rsz, rsz)
                                )
                                drawPath(
                                    Path().apply {

                                        moveTo(cent.x - hperi, cent.y - hperi + csz)
                                        lineTo(cent.x - hperi, cent.y - hperi)
                                        lineTo(cent.x - hperi + csz, cent.y - hperi)

                                        moveTo(cent.x + hperi, cent.y - hperi + csz)
                                        lineTo(cent.x + hperi, cent.y - hperi)
                                        lineTo(cent.x + hperi - csz, cent.y - hperi)

                                        moveTo(cent.x - hperi, cent.y + hperi - csz)
                                        lineTo(cent.x - hperi, cent.y + hperi)
                                        lineTo(cent.x - hperi + csz, cent.y + hperi)

                                        moveTo(cent.x + hperi, cent.y + hperi - csz)
                                        lineTo(cent.x + hperi, cent.y + hperi)
                                        lineTo(cent.x + hperi - csz, cent.y + hperi)

                                    }, color = Color.White, style = Stroke(
                                        width = lwi,
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.cornerPathEffect(rsz)
                                    )
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.weight(20f))
                            Text(text = stringResource(R.string.no_camera_access),
                                color = Color.White, style = Styles.pageTitle,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(R.string.ton_wallet_no_camera),
                                color = Color.White, style = Styles.mainText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp, 0.dp))
                            Spacer(modifier = Modifier.weight(16f))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", act.packageName, null)
                                    intent.setData(uri)
                                    act.startActivity(intent)
                                },
                                modifier = Modifier
                                    .widthIn(200.dp)
                                    .height(Styles.buttonHeight),
                                shape = Styles.buttonShape,
                                colors = ButtonDefaults.buttonColors(Colors.Primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.open_settings),
                                    style = Styles.buttonLabel
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    try {
                                        pickFileLauncher.launch("image/*")
                                    } catch (_: Throwable) {
                                        error("Failed to open pickFileLauncher")
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(200.dp)
                                    .height(Styles.buttonHeight),
                                shape = Styles.buttonShape
                            ) {
                                Text(
                                    text = stringResource(R.string.scan_image_from_gallery),
                                    style = Styles.textButtonLabel, color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.weight(10f))
                        }
                    }
                }
            }
        }
    }

}