package com.ureka.play4change.infrastructure.content

import com.ureka.play4change.application.port.ContentExtractorPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Component
class ContentExtractorAdapter(
    @Value("\${content.scraper-url}") private val scraperUrl: String,
    @Value("\${content.unstructured-url}") private val unstructuredUrl: String
) : ContentExtractorPort {

    private val log = LoggerFactory.getLogger(ContentExtractorAdapter::class.java)

    // Used for URL scraping — scraper has an internal 35 s cap, 45 s is sufficient headroom.
    private val scraperRestTemplate: RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().also {
            it.setConnectTimeout(CONNECT_TIMEOUT_MS)
            it.setReadTimeout(SCRAPER_READ_TIMEOUT_MS)
        }
    )

    // Used for PDF extraction — OCR on large scanned documents can take up to 2 minutes.
    private val unstructuredRestTemplate: RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().also {
            it.setConnectTimeout(CONNECT_TIMEOUT_MS)
            it.setReadTimeout(UNSTRUCTURED_READ_TIMEOUT_MS)
        }
    )

    override fun extractFromUrl(url: String): String {
        UrlSsrfValidator.validate(url)

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(mapOf("url" to url), headers)

        @Suppress("UNCHECKED_CAST")
        val body = try {
            scraperRestTemplate.postForObject("$scraperUrl/scrape", entity, Map::class.java)
                as? Map<String, Any?>
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to fetch URL '$url': ${ex.message}", ex)
        } ?: throw IllegalStateException("Empty response from scraper for '$url'")

        val text = body["text"] as? String
            ?: throw IllegalStateException("Scraper returned no text for '$url'")

        log.debug("Extracted {} chars from URL {}", text.length, url)
        return text
    }

    override fun extractFromPdf(pdfBytes: ByteArray): String {
        val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
        val formBody = LinkedMultiValueMap<String, Any>().apply {
            add("files", object : ByteArrayResource(pdfBytes) {
                override fun getFilename() = "upload.pdf"
            })
            add("strategy", "auto")
        }
        val entity = HttpEntity(formBody, headers)

        @Suppress("UNCHECKED_CAST")
        val elements = try {
            unstructuredRestTemplate.postForObject(
                "$unstructuredUrl/general/v0/general",
                entity,
                List::class.java
            ) as? List<Map<String, Any?>>
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to extract PDF via Unstructured: ${ex.message}", ex)
        } ?: throw IllegalStateException("Unstructured returned null response")

        val text = elements
            .mapNotNull { it["text"] as? String }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
            .replace("\u0000", "")

        log.debug("Extracted {} chars from PDF via Unstructured ({} elements)", text.length, elements.size)
        return text
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val SCRAPER_READ_TIMEOUT_MS = 45_000
        private const val UNSTRUCTURED_READ_TIMEOUT_MS = 120_000
    }
}
