package com.ureka.play4change.auth.adapter.outbound.persistence

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.UserEntity
import com.ureka.play4change.auth.adapter.outbound.persistence.spring.UserJpaRepository
import com.ureka.play4change.auth.AuthProvider
import com.ureka.play4change.auth.domain.model.AuthUser
import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.stereotype.Component

@Component
class UserJpaAdapter(private val jpa: UserJpaRepository) : UserRepository {

    override fun findById(id: String): AuthUser? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): AuthUser? =
        jpa.findByEmail(email)?.toDomain()

    override fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): AuthUser? =
        jpa.findByProviderAndProviderId(provider.name, providerId)?.toDomain()

    override fun save(user: AuthUser): AuthUser =
        jpa.save(user.toEntity()).toDomain()

    override fun existsByEmail(email: String): Boolean =
        jpa.existsByEmail(email)

    private fun UserEntity.toDomain() = AuthUser(
        id = id, email = email, name = name,
        provider = AuthProvider.valueOf(provider),
        providerId = providerId, createdAt = createdAt,
        role = role
    )

    private fun AuthUser.toEntity() = UserEntity(
        id = id, email = email, name = name,
        provider = provider.name, providerId = providerId,
        createdAt = createdAt, role = role
    )
}
