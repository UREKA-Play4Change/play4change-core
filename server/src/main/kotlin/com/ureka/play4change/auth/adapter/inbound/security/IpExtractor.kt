package com.ureka.play4change.auth.adapter.inbound.security

import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress

object IpExtractor {

    /**
     * Extracts the client IP from the request. Uses X-Forwarded-For first header
     * when available. Falls back to remoteAddr.
     *
     * Rejects private/loopback/link-local addresses from X-Forwarded-For to prevent
     * IP spoofing via header injection from internal clients.
     */
    fun extractClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            val candidateIp = forwarded.split(",").first().trim()
            if (candidateIp.isNotBlank() && !isPrivateAddress(candidateIp)) {
                return candidateIp
            }
        }
        return request.remoteAddr ?: "unknown"
    }

    fun isPrivateAddress(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            address.isLoopbackAddress ||
                address.isSiteLocalAddress ||
                address.isLinkLocalAddress ||
                address.isMulticastAddress
        } catch (_: Exception) {
            // Unparseable → treat as untrusted, fall back to remoteAddr
            true
        }
    }
}
