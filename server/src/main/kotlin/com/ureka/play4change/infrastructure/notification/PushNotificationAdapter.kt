package com.ureka.play4change.infrastructure.notification

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.ureka.play4change.application.port.PushNotificationPort
import com.ureka.play4change.domain.notification.DeviceToken
import com.ureka.play4change.domain.notification.DeviceTokenPlatform
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64
import java.util.Date

/**
 * Composite push notification adapter.
 * ANDROID: FCM HTTP v1 via Firebase Admin SDK.
 *   Requires env var: FIREBASE_SERVICE_ACCOUNT_JSON (full service-account JSON as a string).
 * IOS: APNs HTTP/2 via Java 11 HttpClient + JWT (jjwt ES256).
 *   Requires env vars: APNS_KEY_ID, APNS_TEAM_ID, APNS_PRIVATE_KEY (base64-encoded PKCS8 EC key).
 */
@Component
class PushNotificationAdapter(
    @Value("\${fcm.service-account-json:}") private val fcmServiceAccountJson: String,
    @Value("\${apns.key-id:}") private val apnsKeyId: String,
    @Value("\${apns.team-id:}") private val apnsTeamId: String,
    @Value("\${apns.private-key:}") private val apnsPrivateKey: String,
    @Value("\${apns.bundle-id:com.ureka.play4change}") private val apnsBundleId: String,
    @Value("\${apns.production:false}") private val apnsProduction: Boolean
) : PushNotificationPort {

    private val logger = LoggerFactory.getLogger(PushNotificationAdapter::class.java)

    private val firebaseApp: FirebaseApp? by lazy { initFirebase() }
    private val apnsClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(APNS_CONNECT_TIMEOUT_SECONDS))
        .build()

    override fun send(deviceToken: DeviceToken, title: String, body: String) {
        when (deviceToken.platform) {
            DeviceTokenPlatform.ANDROID -> sendFcm(deviceToken.token, title, body)
            DeviceTokenPlatform.IOS -> sendApns(deviceToken.token, title, body)
        }
    }

    private fun sendFcm(token: String, title: String, body: String) {
        val app = firebaseApp ?: run {
            logger.warn("FCM not configured (FIREBASE_SERVICE_ACCOUNT_JSON unset) — skipping")
            return
        }
        val message = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build()
        runCatching { FirebaseMessaging.getInstance(app).send(message) }
            .onSuccess { logger.debug("FCM sent: {}", it) }
            .onFailure { logger.error("FCM send failed: {}", it.message) }
    }

    private fun sendApns(token: String, title: String, body: String) {
        if (apnsKeyId.isBlank() || apnsTeamId.isBlank() || apnsPrivateKey.isBlank()) {
            logger.warn("APNs not configured (APNS_KEY_ID/APNS_TEAM_ID/APNS_PRIVATE_KEY unset) — skipping")
            return
        }
        val host = if (apnsProduction) "api.push.apple.com" else "api.sandbox.push.apple.com"
        val payload = """{"aps":{"alert":{"title":"$title","body":"$body"},"sound":"default"}}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://$host/3/device/$token"))
            .header("authorization", "bearer ${buildApnsJwt()}")
            .header("apns-topic", apnsBundleId)
            .header("apns-push-type", "alert")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
        runCatching {
            val response = apnsClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == HTTP_OK) {
                logger.debug("APNs sent to token ending …{}", token.takeLast(TOKEN_LOG_SUFFIX_LENGTH))
            } else {
                logger.error("APNs push failed: {} {}", response.statusCode(), response.body())
            }
        }.onFailure { logger.error("APNs request error: {}", it.message) }
    }

    private fun buildApnsJwt(): String {
        val privateKeyBytes = Base64.getDecoder().decode(apnsPrivateKey.trim())
        val privateKey = KeyFactory.getInstance("EC")
            .generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        return Jwts.builder()
            .header().add("kid", apnsKeyId).and()
            .issuer(apnsTeamId)
            .issuedAt(Date())
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact()
    }

    private companion object {
        const val APNS_CONNECT_TIMEOUT_SECONDS = 10L
        const val HTTP_OK = 200
        const val TOKEN_LOG_SUFFIX_LENGTH = 8
    }

    private fun initFirebase(): FirebaseApp? {
        if (fcmServiceAccountJson.isBlank()) return null
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(fcmServiceAccountJson.byteInputStream()))
            .build()
        return runCatching { FirebaseApp.getInstance() }
            .getOrElse { FirebaseApp.initializeApp(options) }
    }
}
