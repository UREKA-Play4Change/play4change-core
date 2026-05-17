package com.ureka.play4change.core.camera

import java.io.File

actual fun readPhotoBytes(path: String): ByteArray? =
    runCatching { File(path).readBytes() }.getOrNull()
