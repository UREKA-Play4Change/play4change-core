package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.AuthProvider
import com.ureka.play4change.auth.domain.model.AuthUser

interface UserRepository {
    fun findById(id: String): AuthUser?
    fun findByEmail(email: String): AuthUser?
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): AuthUser?
    fun save(user: AuthUser): AuthUser
    fun existsByEmail(email: String): Boolean
}
