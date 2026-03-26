package com.ureka.play4change.auth.port.outbound

interface EmailPort {
    fun sendMagicLink(toEmail: String, magicLink: String)
}
