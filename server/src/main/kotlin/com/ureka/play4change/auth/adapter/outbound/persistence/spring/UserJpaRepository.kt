package com.ureka.play4change.auth.adapter.outbound.persistence.spring

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun findByEmail(email: String): UserEntity?
    fun findByProviderAndProviderId(provider: String, providerId: String): UserEntity?
    fun existsByEmail(email: String): Boolean
}
