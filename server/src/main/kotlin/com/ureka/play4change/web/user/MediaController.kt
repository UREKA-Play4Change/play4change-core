package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.FileStoragePort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/media")
class MediaController(
    private val fileStorage: FileStoragePort
) {

    @PostMapping("/photo")
    fun uploadPhoto(
        @RequestParam("photo") photo: MultipartFile,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Map<String, String>> {
        val key = "photos/$userId/${UUID.randomUUID()}.jpg"
        val url = fileStorage.uploadFile(key, photo.bytes, photo.contentType ?: "image/jpeg")
        return ResponseEntity.ok(mapOf("url" to url))
    }
}
