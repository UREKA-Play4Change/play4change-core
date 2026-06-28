package com.ureka.play4change.auth.port.outbound

interface EmailPort {
    fun sendMagicLink(toEmail: String, token: String)
    fun sendRecoveryEmailVerification(toEmail: String, token: String)
}
