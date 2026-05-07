package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.peerreview.data.http.HttpPeerReviewRepository
import com.ureka.play4change.features.peerreview.data.mock.MockPeerReviewRepository
import com.ureka.play4change.features.peerreview.domain.repository.PeerReviewRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module

val peerReviewModule = module {
    single<PeerReviewRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockPeerReviewRepository()
        else HttpPeerReviewRepository(get<HttpClient>())
    }
}
