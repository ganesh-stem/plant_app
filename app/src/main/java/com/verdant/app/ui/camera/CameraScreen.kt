package com.verdant.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.verdant.app.R
import com.verdant.app.data.image.ImageStorage
import com.verdant.app.ui.components.drawCornerBrackets
import com.verdant.app.ui.theme.Forest
import com.verdant.app.ui.theme.Lime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(private val storage: ImageStorage) : ViewModel() {
    fun newCaptureFile(): File = storage.newCaptureFile()
}

private enum class PermissionState { UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED }

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onImageCaptured: (Uri) -> Unit,
    onPickFromGallery: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var permission by remember { mutableStateOf(context.cameraPermissionState(askedBefore = false)) }
    var askedBefore by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        askedBefore = true
        permission = if (granted) PermissionState.GRANTED else context.cameraPermissionState(askedBefore = true)
    }

    // Ask automatically the first time; re-check when returning from system settings.
    LaunchedEffect(Unit) {
        if (permission != PermissionState.GRANTED && !askedBefore) launcher.launch(Manifest.permission.CAMERA)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = context.cameraPermissionState(askedBefore)
                if (now == PermissionState.GRANTED || permission == PermissionState.GRANTED) permission = now
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ->
                PermissionPrompt(
                    title = stringResource(R.string.camera_unavailable),
                    body = "",
                    primaryLabel = null,
                    onPrimary = {},
                    onGallery = onPickFromGallery,
                    onClose = onClose,
                    icon = Icons.Rounded.NoPhotography,
                )
            permission == PermissionState.GRANTED ->
                CameraContent(
                    onClose = onClose,
                    onPickFromGallery = onPickFromGallery,
                    onImageCaptured = onImageCaptured,
                    newFile = viewModel::newCaptureFile,
                )
            permission == PermissionState.PERMANENTLY_DENIED ->
                PermissionPrompt(
                    title = stringResource(R.string.camera_permission_title),
                    body = stringResource(R.string.camera_permission_denied_body),
                    primaryLabel = stringResource(R.string.camera_permission_settings),
                    onPrimary = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    onGallery = onPickFromGallery,
                    onClose = onClose,
                )
            else ->
                PermissionPrompt(
                    title = stringResource(R.string.camera_permission_title),
                    body = stringResource(R.string.camera_permission_body),
                    primaryLabel = stringResource(R.string.camera_permission_grant),
                    onPrimary = { launcher.launch(Manifest.permission.CAMERA) },
                    onGallery = onPickFromGallery,
                    onClose = onClose,
                )
        }
    }
}

@Composable
private fun CameraContent(
    onClose: () -> Unit,
    onPickFromGallery: () -> Unit,
    onImageCaptured: (Uri) -> Unit,
    newFile: () -> File,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.camera_error)

    var flashMode by rememberSaveable { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var useFront by rememberSaveable { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    val shutterFlash = remember { Animatable(0f) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    var hasFront by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    LaunchedEffect(useFront) {
        controller.cameraSelector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }
    LaunchedEffect(flashMode) { controller.imageCaptureFlashMode = flashMode }
    // Camera info becomes available asynchronously after binding.
    LaunchedEffect(controller, useFront) {
        controller.initializationFuture.addListener({
            hasFlash = controller.cameraInfo?.hasFlashUnit() == true
            hasFront = runCatching { controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        }, ContextCompat.getMainExecutor(context))
    }

    fun capture() {
        if (capturing) return
        capturing = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shutterFlash.snapTo(0.9f)
            shutterFlash.animateTo(0f, tween(350))
        }
        val file = newFile()
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturing = false
                    onImageCaptured(Uri.fromFile(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    file.delete()
                    scope.launch { snackbar.showSnackbar(errorText) }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        ViewfinderOverlay(Modifier.fillMaxSize())

        // Shutter flash
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shutterFlash.value }
                .background(Color.White),
        )

        // Top controls
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), onClick = onClose)
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.4f)) {
                Text(
                    stringResource(R.string.camera_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            if (hasFlash) {
                val (icon, label) = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn to R.string.camera_flash_on
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto to R.string.camera_flash_auto
                    else -> Icons.Rounded.FlashOff to R.string.camera_flash_off
                }
                GlassIconButton(icon, stringResource(label)) {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        // Bottom controls
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .navigationBarsPadding()
                .padding(bottom = 24.dp, top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.camera_tips),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassIconButton(Icons.Rounded.PhotoLibrary, stringResource(R.string.camera_gallery), size = 56, onClick = onPickFromGallery)
                ShutterButton(capturing = capturing, onClick = ::capture)
                if (hasFront) {
                    GlassIconButton(Icons.Rounded.FlipCameraAndroid, stringResource(R.string.camera_flip), size = 56) { useFront = !useFront }
                } else {
                    Spacer(Modifier.size(56.dp))
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ShutterButton(capturing: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (capturing) 0.86f else 1f, label = "shutter")
    Box(
        Modifier
            .size(84.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            enabled = !capturing,
            shape = CircleShape,
            color = Lime,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (capturing) {
                    CircularProgressIndicator(color = Forest, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = stringResource(R.string.camera_capture), tint = Forest)
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, description: String, size: Int = 48, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

/** Darkens everything outside a rounded square and animates a scanning line inside it. */
@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val line by transition.animateFloat(
        0.08f, 0.92f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "line",
    )
    val pulse by transition.animateFloat(0.96f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")

    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val side = size.width * 0.78f * pulse
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2.3f
        val radius = 36.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.45f))
        drawRoundRect(Color.Transparent, Offset(left, top), Size(side, side), CornerRadius(radius), blendMode = BlendMode.Clear)

        val y = top + side * line
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Lime.copy(alpha = 0.35f)), startY = y - 60.dp.toPx(), endY = y),
            topLeft = Offset(left + 12.dp.toPx(), y - 60.dp.toPx()),
            size = Size(side - 24.dp.toPx(), 60.dp.toPx()),
        )
        drawLine(Lime, Offset(left + 16.dp.toPx(), y), Offset(left + side - 16.dp.toPx(), y), strokeWidth = 2.dp.toPx())

        drawCornerBrackets(
            color = Color.White,
            bounds = Rect(left, top, left + side, top + side),
            arm = 44.dp.toPx(),
            stroke = 4.dp.toPx(),
            radius = radius,
        )
    }
}

@Composable
private fun PermissionPrompt(
    title: String,
    body: String,
    primaryLabel: String?,
    onPrimary: () -> Unit,
    onGallery: () -> Unit,
    onClose: () -> Unit,
    icon: ImageVector = Icons.Rounded.PhotoCamera,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Forest, Color.Black))),
    ) {
        Box(
            Modifier
                .statusBarsPadding()
                .padding(16.dp),
        ) { GlassIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), onClick = onClose) }

        Column(
            Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Lime.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Lime, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
            if (body.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(28.dp))
            if (primaryLabel != null) {
                Button(
                    onClick = onPrimary,
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Forest),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                ) { Text(primaryLabel) }
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = onGallery) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.camera_gallery), color = Color.White)
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.cameraPermissionState(askedBefore: Boolean): PermissionState {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        return PermissionState.GRANTED
    }
    val activity = findActivity() ?: return PermissionState.DENIED
    val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    return when {
        !askedBefore -> PermissionState.UNKNOWN
        canAskAgain -> PermissionState.DENIED
        else -> PermissionState.PERMANENTLY_DENIED
    }
}
