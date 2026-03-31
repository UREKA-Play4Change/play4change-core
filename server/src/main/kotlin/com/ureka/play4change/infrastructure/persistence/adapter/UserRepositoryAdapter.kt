package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.UserEntity
import com.ureka.play4change.auth.adapter.outbound.persistence.spring.UserJpaRepository
import com.ureka.play4change.domain.identity.AuthProvider
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserRepositoryAdapter(
    private val jpa: UserJpaRepository
) : UserRepository {

    override fun findById(id: String): User? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? =
        jpa.findByEmail(email)?.toDomain()

    override fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User? =
        jpa.findByProviderAndProviderId(provider.name, providerId)?.toDomain()

    override fun save(user: User): User {
        val entity = if (user.id.isBlank()) {
            UserEntity(
                id = UUID.randomUUID().toString(),
                email = user.email,
                name = user.name,
                avatarUrl = user.avatarUrl,
                role = user.role.name,
                provider = user.provider.name,
                providerId = user.providerId,
                preferredLanguage = user.preferredLanguage,
                audienceLevel = user.audienceLevel,
                createdAt = user.createdAt
            )
        } else {
            UserEntity(
                id = user.id,
                email = user.email,
                name = user.name,
                avatarUrl = user.avatarUrl,
                role = user.role.name,
                provider = user.provider.name,
                providerId = user.providerId,
                preferredLanguage = user.preferredLanguage,
                audienceLevel = user.audienceLevel,
                createdAt = user.createdAt
            )
        }
        return jpa.save(entity).toDomain()
    }

    private fun UserEntity.toDomain(): User = User(
        id = id,
        email = email,
        name = name,
        avatarUrl = avatarUrl,
        role = UserRole.valueOf(role),
        provider = AuthProvider.valueOf(provider),
        providerId = providerId,
        preferredLanguage = preferredLanguage,
        audienceLevel = audienceLevel,
        createdAt = createdAt
    )
}
