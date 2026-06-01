package com.ureka.play4change.features.auth.platform

import com.ureka.play4change.features.auth.domain.model.SocialProvider

/**
 * Platform-specific launcher that drives the native OAuth dance for a given provider
 * and returns the credential token the server needs to verify:
 *  - Google  → Google ID token (JWT signed by Google)
 *  - Facebook → Facebook user access token
 *
 * Returns null when the user cancels. Throws on unrecoverable errors.
 */
interface SocialAuthLauncher {
    suspend fun launch(provider: SocialProvider): String?
}
