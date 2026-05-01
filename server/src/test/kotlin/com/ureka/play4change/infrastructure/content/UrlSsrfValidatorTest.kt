package com.ureka.play4change.infrastructure.content

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UrlSsrfValidatorTest {

    @Test
    fun `localhost url is rejected`() {
        assertThrows<UrlSsrfViolationException> {
            UrlSsrfValidator.validate("https://localhost/admin")
        }
    }

    @Test
    fun `loopback ip 127_0_0_1 is rejected`() {
        assertThrows<UrlSsrfViolationException> {
            UrlSsrfValidator.validate("https://127.0.0.1/admin")
        }
    }

    @Test
    fun `rfc1918 192_168 address is rejected`() {
        assertThrows<UrlSsrfViolationException> {
            UrlSsrfValidator.validate("https://192.168.1.1/secret")
        }
    }

    @Test
    fun `rfc1918 10_x address is rejected`() {
        assertThrows<UrlSsrfViolationException> {
            UrlSsrfValidator.validate("https://10.0.0.1/internal")
        }
    }

    @Test
    fun `http scheme is rejected`() {
        assertThrows<UrlSsrfViolationException> {
            UrlSsrfValidator.validate("http://example.com/page")
        }
    }

    @Test
    fun `valid public https url is accepted`() {
        // 1.1.1.1 is a well-known public IP — no DNS lookup needed
        UrlSsrfValidator.validate("https://1.1.1.1/")
    }
}
