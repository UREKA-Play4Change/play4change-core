package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.User

interface UserRepository {
    fun findByEmail(email: String): User?
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User?
    fun save(user: User): User
    fun existsByEmail(email: String): Boolean
}
