package com.ureka.play4change.auth.adapter.outbound.persistence

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.UserEntity
import com.ureka.play4change.auth.adapter.outbound.persistence.spring.UserJpaRepository
import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.User
import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.stereotype.Component

@Component
class UserJpaAdapter(private val jpa: UserJpaRepository) : UserRepository {

    override fun findByEmail(email: String): User? =
        jpa.findByEmail(email)?.toDomain()

    override fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User? =
        jpa.findByProviderAndProviderId(provider.name, providerId)?.toDomain()

    override fun save(user: User): User =
        jpa.save(user.toEntity()).toDomain()

    override fun existsByEmail(email: String): Boolean =
        jpa.existsByEmail(email)

    private fun UserEntity.toDomain() = User(
        id = id, email = email, name = name,
        provider = AuthProvider.valueOf(provider),
        providerId = providerId, createdAt = createdAt
    )

    private fun User.toEntity() = UserEntity(
        id = id, email = email, name = name,
        provider = provider.name, providerId = providerId,
        createdAt = createdAt
    )
}
