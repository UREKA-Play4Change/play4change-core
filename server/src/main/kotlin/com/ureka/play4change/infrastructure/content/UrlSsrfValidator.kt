package com.ureka.play4change.infrastructure.content

import java.net.InetAddress
import java.net.URL

/**
 * Validates a URL before fetching to prevent Server-Side Request Forgery (SSRF).
 *
 * Enforces:
 * - HTTPS scheme only
 * - Resolved IP must not be loopback (127.0.0.0/8, ::1)
 * - Resolved IP must not be site-local / RFC 1918 (10.0.0.0/8, 172.16.0.0/12,
 *   192.168.0.0/16)
 * - Resolved IP must not be link-local (169.254.0.0/16, fe80::/10)
 *
 * OWASP A10 — SSRF. See THREAT-LOG.md R06.
 */
object UrlSsrfValidator {

    private const val ALLOWED_SCHEME = "https"

    fun validate(rawUrl: String) {
        val violation = findViolation(rawUrl) ?: return
        throw UrlSsrfViolationException(violation)
    }

    private fun findViolation(rawUrl: String): String? {
        val url = parseUrl(rawUrl) ?: return "Malformed URL: $rawUrl"
        return checkScheme(url) ?: checkHost(url, rawUrl) ?: checkPrivateAddress(url)
    }

    private fun parseUrl(rawUrl: String): URL? = runCatching { URL(rawUrl) }.getOrNull()

    private fun checkScheme(url: URL): String? =
        if (url.protocol != ALLOWED_SCHEME) "Only $ALLOWED_SCHEME scheme is allowed, got '${url.protocol}'" else null

    private fun checkHost(url: URL, rawUrl: String): String? =
        if (url.host.isBlank()) "URL has no host: $rawUrl" else null

    private fun checkPrivateAddress(url: URL): String? {
        val addresses = runCatching { InetAddress.getAllByName(url.host) }
            .getOrElse { return "Cannot resolve host '${url.host}'" }
        return addresses.firstOrNull { it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress }
            ?.let { "URL resolves to a private or reserved IP address: ${it.hostAddress}" }
    }
}
