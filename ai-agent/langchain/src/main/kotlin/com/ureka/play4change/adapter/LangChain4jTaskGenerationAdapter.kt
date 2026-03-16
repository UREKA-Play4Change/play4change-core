package com.ureka.play4change.adapter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ureka.play4change.dedup.PgVectorDeduplicationService
import com.ureka.play4change.dedup.SimilarityMatch
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.server.ServiceUnavailable
import com.ureka.play4change.model.AdaptiveBranch
import com.ureka.play4change.model.GeneratedTask
import com.ureka.play4change.model.GenerationMetadata
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationResult
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.model.ReuseStrategy
import com.ureka.play4change.model.StruggleContext
import com.ureka.play4change.port.TaskGenerationPort
import com.ureka.play4change.prompt.StruggleAnalysisPrompt
import com.ureka.play4change.prompt.TaskGenerationPrompt
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.collections.plus

// ─────────────────────────────────────────────────────────────────────────────
//  LangChain4jTaskGenerationAdapter
//
//  Implements TaskGenerationPort using LangChain4j + Mistral.
//  This is the ONLY class in the entire codebase that knows about LangChain4j.
//
//  To swap to OpenAI: create OpenAiTaskGenerationAdapter implementing
//  TaskGenerationPort, mark it @Primary, done. Zero :server changes.
// ─────────────────────────────────────────────────────────────────────────────
@Service
@Primary
class LangChain4jTaskGenerationAdapter(
    private val chatModel: ChatLanguageModel,
    private val embeddingModel: EmbeddingModel,
    private val deduplicationService: PgVectorDeduplicationService,
    private val jdbc: JdbcTemplate,
    private val meterRegistry: MeterRegistry,
    @Value("\${ai.generation.subtask-count:3}") private val defaultSubtaskCount: Int,
) : TaskGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    // ─────────────────────────────────────────────────────────────────────────
    //  Fixed path task generation
    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun generateTasks(
        request: GenerationRequest
    ): Either<AppError, GenerationResult> {
        val timer = Timer.start(meterRegistry)
        log.info("Generating ${request.taskCount} tasks for domain=${request.subjectDomain} module=${request.moduleId}")

        return runCatching {
            val response = chatModel.generate(
                SystemMessage.from(TaskGenerationPrompt.system()),
                UserMessage.from(TaskGenerationPrompt.user(request))
            )

            val rawJson = response.content().text()
            val tasks = parseTasksFromJson(rawJson, request.moduleId)

            val durationMs = timer.stop(
                meterRegistry.timer("ai.generation.duration", "type", "fixed_path")
            ) / 1_000_000

            GenerationResult(
                tasks = tasks,
                metadata = GenerationMetadata(
                    tasksRequested = request.taskCount,
                    tasksGenerated = tasks.count { it.status == GenerationStatus.SUCCESS },
                    tasksDeduplicated = tasks.count { it.status == GenerationStatus.DUPLICATE },
                    tokensUsed = response.tokenUsage()?.totalTokenCount()?.toLong() ?: 0L,
                    generationTimeMs = durationMs,
                    providerName = "mistral"
                )
            )
        }.fold(
            onSuccess = { it.right() },
            onFailure = { e ->
                log.error("Task generation failed: ${e.message}", e)
                meterRegistry.counter("ai.generation.failures", "type", "fixed_path").increment()
                ServiceUnavailable.DependencyUnavailable("mistral-ai").left()
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Adaptive branch generation with similarity-based reuse
    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun generateAdaptiveBranch(
        context: StruggleContext
    ): Either<AppError, AdaptiveBranch> {
        log.info("Generating adaptive branch for user=${context.userId} task=${context.taskId} errorPattern=${context.errorPattern}")

        return runCatching {
            // 1. Embed the struggle context for similarity search
            val struggleText = "${context.errorPattern} ${context.taskDescription} ${context.subjectDomain}"
            val struggleEmbedding = embeddingModel.embed(struggleText).content().vector()

            // 2. Find similar past struggles
            val match = deduplicationService.findSimilarStruggle(struggleEmbedding)

            // 3. Apply similarity-based reuse strategy
            when (match.strategy) {
                ReuseStrategy.FULL_REUSE -> {
                    log.info("Full reuse: similarity=${match.similarity} branchId=${match.branchId}")
                    meterRegistry.counter("ai.adaptive.reuse", "strategy", "full").increment()
                    loadExistingBranch(match.branchId!!, ReuseStrategy.FULL_REUSE)
                }

                ReuseStrategy.PARTIAL_REUSE -> {
                    log.info("Partial reuse: similarity=${match.similarity} branchId=${match.branchId}")
                    meterRegistry.counter("ai.adaptive.reuse", "strategy", "partial").increment()
                    mergeAndGenerate(match, context)
                }

                ReuseStrategy.FRESH_GENERATION -> {
                    log.info("Fresh generation: no similar struggle found")
                    meterRegistry.counter("ai.adaptive.reuse", "strategy", "fresh").increment()
                    generateFreshBranch(context, struggleEmbedding)
                }
            }
        }.fold(
            onSuccess = { it.right() },
            onFailure = { e ->
                log.error("Adaptive branch generation failed: ${e.message}", e)
                meterRegistry.counter("ai.generation.failures", "type", "adaptive").increment()
                ServiceUnavailable.DependencyUnavailable("mistral-ai").left()
            }
        )
    }

    override suspend fun healthCheck(): Boolean {
        return runCatching {
            chatModel.generate(UserMessage.from("Respond with: ok"))
            true
        }.getOrElse { false }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadExistingBranch(branchId: String, strategy: ReuseStrategy): AdaptiveBranch {
        val tasks = jdbc.queryForList(
            """
            SELECT id, title, description, hint, points_reward 
            FROM adaptive_tasks WHERE branch_id = ? ORDER BY order_index
            """.trimIndent(),
            branchId
        ).map { row ->
            GeneratedTask(
                externalId = row["id"] as String,
                title = row["title"] as String,
                description = row["description"] as String,
                hint = row["hint"] as String,
                pointsReward = (row["points_reward"] as Number).toInt(),
                embedding = FloatArray(0), // already stored, not needed
                status = GenerationStatus.SUCCESS
            )
        }
        return AdaptiveBranch(
            branchId = UUID.randomUUID().toString(),
            subtasks = tasks,
            reuseStrategy = strategy,
            reusedFromBranchId = branchId
        )
    }

    private fun mergeAndGenerate(match: SimilarityMatch, context: StruggleContext): AdaptiveBranch {
        val existing = loadExistingBranch(match.branchId!!, ReuseStrategy.PARTIAL_REUSE)
        val reuseCount = (existing.subtasks.size * match.similarity).toInt().coerceAtLeast(1)
        val reusedTasks = existing.subtasks.take(reuseCount)
        val generateCount = defaultSubtaskCount - reuseCount

        val freshTasks = if (generateCount > 0) {
            val response = chatModel.generate(
                SystemMessage.from(StruggleAnalysisPrompt.system()),
                UserMessage.from(StruggleAnalysisPrompt.user(context, generateCount))
            )
            parseTasksFromJson(response.content().text(), context.moduleId)
        } else emptyList()

        return AdaptiveBranch(
            branchId = UUID.randomUUID().toString(),
            subtasks = reusedTasks + freshTasks,
            reuseStrategy = ReuseStrategy.PARTIAL_REUSE,
            reusedFromBranchId = match.branchId
        )
    }

    private fun generateFreshBranch(context: StruggleContext, embedding: FloatArray): AdaptiveBranch {
        val response = chatModel.generate(
            SystemMessage.from(StruggleAnalysisPrompt.system()),
            UserMessage.from(StruggleAnalysisPrompt.user(context, defaultSubtaskCount))
        )
        val tasks = parseTasksFromJson(response.content().text(), context.moduleId)
        return AdaptiveBranch(
            branchId = UUID.randomUUID().toString(),
            subtasks = tasks,
            reuseStrategy = ReuseStrategy.FRESH_GENERATION,
            reusedFromBranchId = null
        )
    }

    private fun parseTasksFromJson(rawJson: String, moduleId: String): List<GeneratedTask> {
        val json = Json { ignoreUnknownKeys = true }
        val cleaned = rawJson.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val array = json.parseToJsonElement(cleaned).jsonArray

        return array.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val title = obj["title"]?.jsonPrimitive?.content ?: return@runCatching null
                val description = obj["description"]?.jsonPrimitive?.content ?: return@runCatching null
                val hint = obj["hint"]?.jsonPrimitive?.content ?: return@runCatching null
                val points = obj["pointsReward"]?.jsonPrimitive?.int ?: 20
                val optionsJsonStr = obj["options"]?.jsonArray?.toString()
                val correctIdx = obj["correctAnswerIndex"]?.jsonPrimitive?.int ?: 0

                // Embed for deduplication check
                val embedding = embeddingModel.embed("$title $description").content().vector()

                val isDuplicate = deduplicationService.isDuplicate(embedding, moduleId)

                GeneratedTask(
                    externalId = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    hint = hint,
                    pointsReward = points,
                    embedding = embedding,
                    status = if (isDuplicate) GenerationStatus.DUPLICATE else GenerationStatus.SUCCESS,
                    optionsJson = optionsJsonStr,
                    correctAnswerIndex = correctIdx
                )
            }.getOrNull()
        }
    }
}