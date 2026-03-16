package com.ureka.play4change.features.home.domain.repository

import com.ureka.play4change.features.home.domain.model.HomeData

interface HomeRepository {
    suspend fun getHomeData(userId: String): HomeData
}
