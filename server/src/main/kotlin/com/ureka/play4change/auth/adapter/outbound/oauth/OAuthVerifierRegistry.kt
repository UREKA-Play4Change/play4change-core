package com.ureka.play4change.auth.adapter.outbound.oauth

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.OAuthClaims
import com.ureka.play4change.auth.port.outbound.OAuthVerifierPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class OAuthVerifierRegistry(
    @Qualifier("googleOAuthAdapter") private val google: OAuthVerifierPort,
    @Qualifier("facebookOAuthAdapter") private val facebook: OAuthVerifierPort
) : OAuthVerifierPort {

    override fun verify(provider: AuthProvider, idToken: String): OAuthClaims = when (provider) {
        AuthProvider.GOOGLE -> google.verify(provider, idToken)
        AuthProvider.FACEBOOK -> facebook.verify(provider, idToken)
        AuthProvider.MAGIC_LINK ->
            throw IllegalArgumentException("MAGIC_LINK is not an OAuth provider")
    }
}
