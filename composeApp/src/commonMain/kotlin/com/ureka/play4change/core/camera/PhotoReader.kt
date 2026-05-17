package com.ureka.play4change.core.camera

/** Reads the raw bytes of a photo at the given file-system [path]. Returns null on failure. */
expect fun readPhotoBytes(path: String): ByteArray?
