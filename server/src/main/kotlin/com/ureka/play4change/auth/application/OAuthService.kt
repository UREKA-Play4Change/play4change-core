package com.ureka.play4change.auth.application

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.domain.model.User
import com.ureka.play4change.auth.port.inbound.OAuthUseCase
import com.ureka.play4change.auth.port.outbound.OAuthVerifierPort
import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class OAuthService(
    private val oAuthVerifierPort: OAuthVerifierPort,
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) : OAuthUseCase {

    override fun loginOrRegister(provider: AuthProvider, idToken: String): TokenPair {
        val claims = oAuthVerifierPort.verify(provider, idToken)

        val user = userRepository.findByProviderAndProviderId(provider, claims.sub)
            ?: userRepository.findByEmail(claims.email)
            ?: userRepository.save(
                User(
                    id = UUID.randomUUID().toString(),
                    email = claims.email,
                    name = claims.name,
                    provider = provider,
                    providerId = claims.sub,
                    createdAt = OffsetDateTime.now()
                )
            )

        return tokenService.issue(user.id, user.email, user.role)
    }
}
