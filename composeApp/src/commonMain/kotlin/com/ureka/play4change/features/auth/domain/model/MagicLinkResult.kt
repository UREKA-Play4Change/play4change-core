package com.ureka.play4change.features.auth.domain.model

/**
 * Result of requesting a magic link.
 * success = true means the email was accepted and the link was sent.
 * The actual tokens come later via verifyMagicLink().
 */
data class MagicLinkResult(val success: Boolean)
