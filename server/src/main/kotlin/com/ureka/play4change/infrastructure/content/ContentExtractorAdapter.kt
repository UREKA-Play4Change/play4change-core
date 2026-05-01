package com.ureka.play4change.infrastructure.content

import com.ureka.play4change.application.port.ContentExtractorPort
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ContentExtractorAdapter : ContentExtractorPort {

    private val log = LoggerFactory.getLogger(ContentExtractorAdapter::class.java)

    private val restTemplate: RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().also {
            it.setConnectTimeout(CONNECT_TIMEOUT_MS)
            it.setReadTimeout(READ_TIMEOUT_MS)
        }
    )

    override fun extractFromUrl(url: String): String {
        UrlSsrfValidator.validate(url)

        val response = try {
            restTemplate.getForObject(url, String::class.java)
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to fetch URL '$url': ${ex.message}", ex)
        }
        val html = response ?: throw IllegalStateException("Empty response from $url")
        val doc = Jsoup.parse(html, url)
        doc.select("script, style, nav, footer, header, aside, noscript").remove()
        val text = doc.body().text().trim()
        log.debug("Extracted {} chars from URL {}", text.length, url)
        return text
    }

    override fun extractFromPdf(pdfBytes: ByteArray): String {
        return Loader.loadPDF(pdfBytes).use { doc ->
            val stripper = PDFTextStripper()
            val text = stripper.getText(doc).trim()
            log.debug("Extracted {} chars from PDF ({} pages)", text.length, doc.getNumberOfPages())
            text
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
