package com.ureka.play4change.auth.adapter.outbound.persistence.spring

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.MagicLinkTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface MagicLinkTokenJpaRepository : JpaRepository<MagicLinkTokenEntity, String> {
    fun findByToken(token: String): MagicLinkTokenEntity?

    @Modifying
    @Transactional
    @Query("UPDATE MagicLinkTokenEntity m SET m.used = true WHERE m.id = :id")
    fun markUsed(id: String)
}
