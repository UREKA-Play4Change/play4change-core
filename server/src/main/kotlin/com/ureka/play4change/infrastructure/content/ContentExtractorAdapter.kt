package com.ureka.play4change.infrastructure.content

import com.ureka.play4change.application.port.ContentExtractorPort
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ContentExtractorAdapter : ContentExtractorPort {

    private val log = LoggerFactory.getLogger(ContentExtractorAdapter::class.java)
    private val restTemplate = RestTemplate()

    override fun extractFromUrl(url: String): String {
        val html = try {
            restTemplate.getForObject(url, String::class.java)
                ?: throw IllegalStateException("Empty response from $url")
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to fetch URL '$url': ${ex.message}", ex)
        }
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
}
