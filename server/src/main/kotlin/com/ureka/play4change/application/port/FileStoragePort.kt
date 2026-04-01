package com.ureka.play4change.application.port

interface FileStoragePort {
    /** Upload bytes under [key] and return the public URL. */
    fun uploadFile(key: String, bytes: ByteArray, contentType: String): String

    /** Download the object at [key] and return its raw bytes. */
    fun downloadFile(key: String): ByteArray

    /** Delete the object at [key]. No-op if the object does not exist. */
    fun deleteFile(key: String)
}
