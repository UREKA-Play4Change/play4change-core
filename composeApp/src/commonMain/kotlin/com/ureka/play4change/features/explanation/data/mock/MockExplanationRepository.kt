package com.ureka.play4change.features.explanation.data.mock

import com.ureka.play4change.features.explanation.domain.model.ExplanationMessage
import com.ureka.play4change.features.explanation.domain.model.ExplanationSession
import com.ureka.play4change.features.explanation.domain.repository.ExplanationRepository
import kotlinx.coroutines.delay

class MockExplanationRepository : ExplanationRepository {

    override suspend fun getSession(sessionId: String): ExplanationSession {
        delay(600)
        return ExplanationSession(
            sessionId = sessionId,
            status = "ACTIVE",
            explanationText = "Let's break this down clearly. The concept you struggled with is about recycling — specifically which materials can be recycled and why. Glass, paper, and hard plastic can be recycled because they can be melted or pulped and reformed. Soft plastics and food scraps cannot, as they contaminate the recycling stream.\n\nIn your practice sessions, the most common mistake was confusing biodegradable with recyclable — these are different properties! Something biodegradable breaks down naturally, while something recyclable can be processed and reused.\n\nThe key rule: if it's a rigid container (glass, metal, hard plastic) or dry paper — recycle it. If it's soft, food-soiled, or organic — compost or landfill.",
            messages = emptyList()
        )
    }

    override suspend fun sendMessage(sessionId: String, content: String): ExplanationMessage {
        delay(800)
        return ExplanationMessage(
            id = "mock-ai-reply-001",
            role = "AI",
            content = "Great question! Think of it this way: soft plastics like bags and film are excluded because recycling machines cannot process them — they jam the sorting equipment. That's why they go to landfill or special drop-off points, not your curbside bin."
        )
    }

    override suspend fun resolve(sessionId: String) {
        delay(200)
    }
}
