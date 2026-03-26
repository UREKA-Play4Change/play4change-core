package com.ureka.play4change.auth.adapter.outbound.persistence.spring

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, String> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity r SET r.used = true WHERE r.id = :id")
    fun markUsed(id: String)

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity r SET r.used = true WHERE r.userId = :userId")
    fun revokeAllByUserId(userId: String)

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity r SET r.used = true WHERE r.familyId = :familyId")
    fun revokeAllByFamilyId(familyId: String)
}
