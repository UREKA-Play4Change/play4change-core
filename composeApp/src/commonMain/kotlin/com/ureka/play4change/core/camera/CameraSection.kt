package com.ureka.play4change.core.camera

import androidx.compose.runtime.Composable

@Composable
expect fun CameraSection(capturedUri: String?, onCapture: (String) -> Unit)
