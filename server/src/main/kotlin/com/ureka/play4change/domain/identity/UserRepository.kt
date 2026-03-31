package com.ureka.play4change.domain.identity

interface UserRepository {
    fun findById(id: String): User?
    fun findByEmail(email: String): User?
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User?
    fun save(user: User): User
}
