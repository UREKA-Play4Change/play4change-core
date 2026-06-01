package com.ureka.play4change.features.auth.platform

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ureka.play4change.features.auth.domain.model.SocialProvider

/**
 * Android implementation of [SocialAuthLauncher].
 *
 * Google  — uses Android Credential Manager (One Tap / bottom-sheet picker).
 *           Returns a Google ID token whose `aud` matches the Web Client ID the
 *           server already validates against.
 *
 * Facebook — delegates to [SocialLoginController] (Facebook Login SDK).
 *            Returns a Facebook user access token.
 *
 * @param serverClientId  The OAuth 2.0 *Web* client ID from the Google Cloud Console.
 *                        The server verifies the token against this same value.
 */
class AndroidSocialAuthLauncher(
    private val serverClientId: String,
) : SocialAuthLauncher {

    override suspend fun launch(provider: SocialProvider): String? = when (provider) {
        SocialProvider.GOOGLE -> launchGoogle()
        SocialProvider.FACEBOOK -> launchFacebook()
    }

    private suspend fun launchGoogle(): String? {
        val activity = ActivityHolder.require()
        val credentialManager = CredentialManager.create(activity)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
        return try {
            val result = credentialManager.getCredential(activity, request)
            when (val cred = result.credential) {
                is GoogleIdTokenCredential -> cred.idToken
                else -> null
            }
        } catch (_: GetCredentialCancellationException) {
            null
        }
    }

    private suspend fun launchFacebook(): String? {
        val controller = SocialLoginController.current
            ?: error("SocialLoginController not initialised — ensure MainActivity.onCreate() ran")
        return controller.facebookLogin()
    }
}
