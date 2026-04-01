package com.ureka.play4change.application.port

interface ContentExtractorPort {
    /** Fetch the page at [url] and return its readable plain text. */
    fun extractFromUrl(url: String): String

    /** Extract readable plain text from the given PDF bytes. */
    fun extractFromPdf(pdfBytes: ByteArray): String
}
