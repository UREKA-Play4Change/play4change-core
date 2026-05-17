package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.DeviceTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface DeviceTokenJpaRepository : JpaRepository<DeviceTokenEntity, String> {
    fun findByUserIdAndPlatform(userId: String, platform: String): DeviceTokenEntity?
    fun findByUserId(userId: String): List<DeviceTokenEntity>

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.userId = :userId")
    fun deleteAllByUserId(userId: String)
}
