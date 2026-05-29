package com.ureka.play4change.application.topic

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.ContentExtractorPort
import com.ureka.play4change.application.port.CreatePdfTopicCommand
import com.ureka.play4change.application.port.CreateUrlTopicCommand
import com.ureka.play4change.application.port.FileStoragePort
import com.ureka.play4change.application.port.LearningGraph
import com.ureka.play4change.application.port.PrerequisiteEdge
import com.ureka.play4change.application.port.TopicDetail
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicPhaseLogRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStats
import com.ureka.play4change.domain.topic.TopicStatsRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.error.server.InternalServerError
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TopicManagementService(
    private val topicRepository: TopicRepository,
    private val fileStoragePort: FileStoragePort,
    private val contentExtractorPort: ContentExtractorPort,
    private val orchestrator: TaskGenerationOrchestrator,
    private val phaseTransitionService: PhaseTransitionService,
    private val phaseLogRepository: TopicPhaseLogRepository,
    private val statsRepository: TopicStatsRepository,
    private val prerequisiteRepository: PrerequisiteRepository
) : TopicUseCase {

    private val log = LoggerFactory.getLogger(TopicManagementService::class.java)

    override fun createFromUrl(command: CreateUrlTopicCommand, adminId: String): Either<AppError, Topic> = either {
        ensure(command.subscriptionWindowDays >= 3) {
            BadRequest.InvalidField("subscriptionWindowDays", "must be at least 3")
        }
        ensure(command.taskCount > 0) {
            BadRequest.InvalidField("taskCount", "must be greater than 0")
        }

        val rawText = try {
            contentExtractorPort.extractFromUrl(command.url)
        } catch (ex: Exception) {
            log.warn("Failed to fetch URL content for topic creation: {}", ex.message)
            raise(BadRequest.InvalidField("url", "could not fetch content: ${ex.message}"))
        }

        ensure(rawText.isNotBlank()) {
            BadRequest.InvalidField("url", "page returned no extractable text")
        }

        val topicId = UUID.randomUUID().toString()
        val storageKey = "topics/$topicId/content.txt"
        val contentRef = try {
            fileStoragePort.uploadFile(storageKey, rawText.toByteArray(Charsets.UTF_8), "text/plain")
        } catch (ex: Exception) {
            log.error("MinIO upload failed for topic {}: {}", topicId, ex.message)
            raise(InternalServerError.UnexpectedException)
        }

        val now = OffsetDateTime.now()
        val topic = topicRepository.save(
            Topic(
                id = topicId,
                title = command.title,
                description = command.description,
                category = command.category,
                contentSourceType = ContentSourceType.URL,
                contentSourceRef = contentRef,
                rawExtractedText = rawText,
                taskCount = command.taskCount,
                subscriptionWindowDays = command.subscriptionWindowDays,
                expiresAt = command.expiresAt,
                audienceLevel = command.audienceLevel,
                language = command.language,
                status = TopicStatus.PENDING,
                createdBy = adminId,
                createdAt = now,
                currentPhase = GenerationPhase.INGESTION,
                phaseUpdatedAt = now
            )
        )

        log.info("Topic {} created from URL by admin {}", topicId, adminId)
        orchestrator.generateAsync(topicId)
        topic
    }

    override fun createFromPdf(command: CreatePdfTopicCommand, adminId: String): Either<AppError, Topic> = either {
        ensure(command.subscriptionWindowDays >= 3) {
            BadRequest.InvalidField("subscriptionWindowDays", "must be at least 3")
        }
        ensure(command.taskCount > 0) {
            BadRequest.InvalidField("taskCount", "must be greater than 0")
        }
        ensure(command.pdfBytes.isNotEmpty()) {
            BadRequest.InvalidField("file", "PDF file is empty")
        }

        val rawText = try {
            contentExtractorPort.extractFromPdf(command.pdfBytes)
        } catch (ex: Exception) {
            log.warn("PDF extraction failed: {}", ex.message)
            raise(BadRequest.InvalidField("file", "could not extract text from PDF: ${ex.message}"))
        }

        ensure(rawText.isNotBlank()) {
            BadRequest.InvalidField("file", "PDF contains no extractable text")
        }

        val topicId = UUID.randomUUID().toString()
        val storageKey = "topics/$topicId/content.pdf"
        val contentRef = try {
            fileStoragePort.uploadFile(storageKey, command.pdfBytes, "application/pdf")
        } catch (ex: Exception) {
            log.error("MinIO upload failed for topic {}: {}", topicId, ex.message)
            raise(InternalServerError.UnexpectedException)
        }

        val now = OffsetDateTime.now()
        val topic = topicRepository.save(
            Topic(
                id = topicId,
                title = command.title,
                description = command.description,
                category = command.category,
                contentSourceType = ContentSourceType.PDF,
                contentSourceRef = contentRef,
                rawExtractedText = rawText,
                taskCount = command.taskCount,
                subscriptionWindowDays = command.subscriptionWindowDays,
                expiresAt = command.expiresAt,
                audienceLevel = command.audienceLevel,
                language = command.language,
                status = TopicStatus.PENDING,
                createdBy = adminId,
                createdAt = now,
                currentPhase = GenerationPhase.INGESTION,
                phaseUpdatedAt = now
            )
        )

        log.info("Topic {} created from PDF '{}' by admin {}", topicId, command.fileName, adminId)
        orchestrator.generateAsync(topicId)
        topic
    }

    override fun getById(topicId: String): Either<AppError, Topic> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
    }

    override fun getByIdWithLog(topicId: String): Either<AppError, TopicDetail> = either {
        val topic = ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        val stats = if (topic.status == TopicStatus.ACTIVE) statsRepository.getForTopic(topicId) else null
        TopicDetail(topic, phaseLogRepository.findByTopicId(topicId), stats)
    }

    override fun listAll(statusFilter: String?, page: Int, size: Int): PageResult<TopicDetail> {
        val pageResult = if (statusFilter != null) {
            val status = TopicStatus.valueOf(statusFilter.uppercase())
            topicRepository.findByStatus(status, page, size)
        } else {
            topicRepository.findAll(page, size)
        }
        val activeIds = pageResult.content.filter { it.status == TopicStatus.ACTIVE }.map { it.id }
        val statsMap: Map<String, TopicStats> = if (activeIds.isNotEmpty()) {
            val fetched = statsRepository.getForTopics(activeIds)
            activeIds.associateWith { id -> fetched[id] ?: TopicStats(0, 0.0, 0.0, 0) }
        } else emptyMap()
        return PageResult(
            content = pageResult.content.map { topic ->
                TopicDetail(topic = topic, generationLog = emptyList(), stats = statsMap[topic.id])
            },
            page = pageResult.page,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    override fun regenerate(topicId: String, adminId: String): Either<AppError, Topic> = either {
        val topic = ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        ensure(topic.status != TopicStatus.GENERATING) {
            Conflict.ConcurrentModification
        }

        log.info("Regeneration of topic {} requested by admin {}", topicId, adminId)
        phaseTransitionService.transitionTo(topicId, GenerationPhase.INGESTION)
        orchestrator.generateAsync(topicId)
        topicRepository.findById(topicId) ?: topic
    }

    override fun getPrerequisites(topicId: String): Either<AppError, List<Topic>> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        val prereqIds = prerequisiteRepository.findPrerequisitesByTopicId(topicId)
        prereqIds.mapNotNull { topicRepository.findById(it) }
    }

    override fun setPrerequisites(topicId: String, prerequisiteIds: List<String>): Either<AppError, List<Topic>> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        ensure(topicId !in prerequisiteIds) {
            BadRequest.InvalidField("prerequisiteIds", "a topic cannot be its own prerequisite")
        }
        prerequisiteIds.forEach { prereqId ->
            ensureNotNull(topicRepository.findById(prereqId)) {
                NotFound.ResourceNotFound("Topic", prereqId)
            }
        }
        ensure(!hasCycle(topicId, prerequisiteIds)) {
            BadRequest.InvalidField("prerequisiteIds", "setting these prerequisites would create a cycle in the learning graph")
        }
        prerequisiteRepository.setPrerequisites(topicId, prerequisiteIds)
        prerequisiteIds.mapNotNull { topicRepository.findById(it) }
    }

    override fun getLearningGraph(): LearningGraph {
        val edges = prerequisiteRepository.findAllEdges()
            .map { (topicId, prereqId) -> PrerequisiteEdge(topicId, prereqId) }
        return LearningGraph(edges)
    }

    /** BFS cycle detection: returns true if adding [newPrereqs] for [topicId] would create a cycle. */
    private fun hasCycle(topicId: String, newPrereqs: List<String>): Boolean {
        // Build the current graph, replacing edges for topicId with newPrereqs
        val adjacency = mutableMapOf<String, MutableList<String>>()
        prerequisiteRepository.findAllEdges().forEach { (t, p) ->
            if (t != topicId) adjacency.getOrPut(t) { mutableListOf() }.add(p)
        }
        adjacency[topicId] = newPrereqs.toMutableList()

        // DFS from every node to detect a back-edge reaching topicId
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(node: String): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited.add(node)
            inStack.add(node)
            for (neighbor in adjacency[node] ?: emptyList()) {
                if (dfs(neighbor)) return true
            }
            inStack.remove(node)
            return false
        }

        val allNodes = adjacency.keys + adjacency.values.flatten()
        return allNodes.any { node -> node !in visited && dfs(node) }
    }
}
