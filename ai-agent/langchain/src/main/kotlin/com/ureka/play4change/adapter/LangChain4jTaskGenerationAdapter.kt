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
import com.ureka.play4change.model.ConversationMessage
import com.ureka.play4change.model.ExplanationContext
import com.ureka.play4change.model.StruggleContext
import com.ureka.play4change.port.TaskGenerationPort
import com.ureka.play4change.prompt.ExplanationPrompt
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
    @Value("\${ai.dedup.partial-reuse-threshold:0.65}") private val partialReuseThreshold: Double,
    @Value("\${ai.dedup.full-reuse-threshold:0.90}") private val fullReuseThreshold: Double,
) : TaskGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    // ─────────────────────────────────────────────────────────────────────────
    //  Fixed path task generation
    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun generateTasks(
        request: GenerationRequest
    ): Either<AppError, GenerationResult> {
        val generationTimer = Timer.start(meterRegistry)
        log.info("Generating ${request.taskCount} tasks for domain=${request.subjectDomain} module=${request.moduleId}")

        return runCatching {
            val systemMsg = SystemMessage.from(TaskGenerationPrompt.system(request.language))
            val userMsg = UserMessage.from(TaskGenerationPrompt.user(request))

            var response = chatModel.generate(systemMsg, userMsg)
            var tasks = parseTasksFromJson(response.content().text(), request.moduleId, request.onTaskGenerated, request.taskCount)
            var tokensUsed = response.tokenUsage()?.totalTokenCount()?.toLong() ?: 0L

            // Schema validation: retry once with a reminder if tasks are missing
            if (tasks.count { it.status == GenerationStatus.SUCCESS } < request.taskCount) {
                log.warn(
                    "Schema validation failed for module {}: got {} valid tasks, expected {}. Retrying.",
                    request.moduleId,
                    tasks.count { it.status == GenerationStatus.SUCCESS },
                    request.taskCount
                )
                val retryUserMsg = UserMessage.from(
                    TaskGenerationPrompt.user(request) + "\n\n" +
                        TaskGenerationPrompt.schemaReminder(request.taskCount)
                )
                response = chatModel.generate(systemMsg, retryUserMsg)
                tasks = parseTasksFromJson(response.content().text(), request.moduleId, request.onTaskGenerated, request.taskCount)
                tokensUsed += response.tokenUsage()?.totalTokenCount()?.toLong() ?: 0L
            }

            if (tasks.none { it.status == GenerationStatus.SUCCESS }) {
                meterRegistry.counter("ai.generation.failures", "type", "schema_validation").increment()
                return ServiceUnavailable.DependencyUnavailable("mistral-ai-schema").left()
            }

            val durationMs = generationTimer.stop(
                meterRegistry.timer(
                    "ai.generation.duration",
                    "generation_phase", "GENERATION"
                )
            ) / 1_000_000

            GenerationResult(
                tasks = tasks,
                metadata = GenerationMetadata(
                    tasksRequested = request.taskCount,
                    tasksGenerated = tasks.count { it.status == GenerationStatus.SUCCESS },
                    tasksDeduplicated = tasks.count { it.status == GenerationStatus.DUPLICATE },
                    tokensUsed = tokensUsed,
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
        val analysisTimer = Timer.start(meterRegistry)
        log.info("Generating adaptive branch for user=${context.userId} task=${context.taskId} errorPattern=${context.errorPattern}")

        return runCatching {
            // 1. Embed the struggle context for similarity search
            val struggleText = "${context.errorPattern} ${context.taskDescription} ${context.subjectDomain}"
            val struggleEmbedding = embeddingModel.embed(struggleText).content().vector()
            require(struggleEmbedding.size == 1024) {
                "Expected 1024-dimensional embedding from Mistral, got ${struggleEmbedding.size}"
            }

            // 2. Find similar past struggles, excluding branches this learner has already seen
            val match = deduplicationService.findSimilarStruggle(struggleEmbedding, context.excludedBranchIds)

            // 3. Apply similarity-based reuse strategy
            val result = when (match.strategy) {
                ReuseStrategy.FULL_REUSE -> {
                    log.info("Full reuse: similarity=${match.similarity} branchId=${match.branchId}")
                    meterRegistry.counter("ai.adaptive.reuse", "strategy", "full").increment()
                    loadExistingBranch(match.branchId!!, ReuseStrategy.FULL_REUSE)
                }

                ReuseStrategy.PARTIAL_REUSE -> {
                    log.info("Partial reuse: similarity=${match.similarity} branchId=${match.branchId}")
                    meterRegistry.counter("ai.adaptive.reuse", "strategy", "partial").increment()
                    mergeAndGenerate(match, context, struggleEmbedding)
                }

                ReuseStrategy.FRESH_GENERATION -> {
                    log.info("Fresh generation: no similar struggle found")
                    meterRegistry.counter("ai.adaptive.reuse", "strategy", "fresh").increment()
                    generateFreshBranch(context, struggleEmbedding)
                }
            }
            analysisTimer.stop(
                meterRegistry.timer(
                    "ai.generation.duration",
                    "generation_phase", "ANALYSIS"
                )
            )
            result
        }.fold(
            onSuccess = { it.right() },
            onFailure = { e ->
                log.error("Adaptive branch generation failed: ${e.message}", e)
                meterRegistry.counter("ai.generation.failures", "type", "adaptive").increment()
                ServiceUnavailable.DependencyUnavailable("mistral-ai").left()
            }
        )
    }

    override suspend fun generateExplanation(context: ExplanationContext): Either<AppError, String> =
        runCatching {
            val systemMsg = SystemMessage.from(ExplanationPrompt.systemExplanation())
            val userMsg = UserMessage.from(ExplanationPrompt.userExplanation(context))
            chatModel.generate(systemMsg, userMsg).content().text().trim()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { e ->
                log.error("Explanation generation failed: ${e.message}", e)
                meterRegistry.counter("ai.generation.failures", "type", "explanation").increment()
                ServiceUnavailable.DependencyUnavailable("mistral-ai-explanation").left()
            }
        )

    override suspend fun generateExplanationReply(
        context: ExplanationContext,
        history: List<ConversationMessage>,
        userMessage: String
    ): Either<AppError, String> =
        runCatching {
            val systemMsg = SystemMessage.from(ExplanationPrompt.systemReply())
            val userMsg = UserMessage.from(ExplanationPrompt.userReply(context, history, userMessage))
            chatModel.generate(systemMsg, userMsg).content().text().trim()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { e ->
                log.error("Explanation reply generation failed: ${e.message}", e)
                meterRegistry.counter("ai.generation.failures", "type", "explanation_reply").increment()
                ServiceUnavailable.DependencyUnavailable("mistral-ai-explanation").left()
            }
        )

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
            SELECT DISTINCT ON (title) id, title, description, hint, points_reward, options, correct_answer
            FROM adaptive_tasks WHERE branch_id = ? ORDER BY title, order_index
            """.trimIndent(),
            branchId
        ).mapNotNull { row ->
            // Skip rows with no options — they pre-date the options validation fix or
            // were stored from a malformed AI response and must not be reused.
            val optionsStr = row["options"]?.toString() ?: return@mapNotNull null
            val optionCount = runCatching {
                Json.parseToJsonElement(optionsStr).jsonArray.size
            }.getOrElse { 0 }
            if (optionCount < 2) return@mapNotNull null
            GeneratedTask(
                externalId = row["id"] as String,
                title = row["title"] as String,
                description = row["description"] as String,
                hint = row["hint"] as? String ?: "",
                pointsReward = (row["points_reward"] as Number).toInt(),
                embedding = FloatArray(0),
                status = GenerationStatus.SUCCESS,
                optionsJson = optionsStr,
                correctAnswerIndex = (row["correct_answer"] as? Number)?.toInt() ?: 0
            )
        }
        return AdaptiveBranch(
            branchId = branchId,
            subtasks = tasks,
            reuseStrategy = strategy,
            reusedFromBranchId = branchId
        )
    }

    private fun mergeAndGenerate(match: SimilarityMatch, context: StruggleContext, embedding: FloatArray): AdaptiveBranch {
        val existing = loadExistingBranch(match.branchId!!, ReuseStrategy.PARTIAL_REUSE)
        val normalizedSimilarity = ((match.similarity - partialReuseThreshold) / (fullReuseThreshold - partialReuseThreshold)).coerceIn(0.0, 1.0)
        val reuseCount = (existing.subtasks.size * normalizedSimilarity).toInt().coerceAtLeast(1)
        val reusedTasks = existing.subtasks.take(reuseCount)
        val generateCount = defaultSubtaskCount - reuseCount

        val freshTasks = if (generateCount > 0) {
            val response = chatModel.generate(
                SystemMessage.from(StruggleAnalysisPrompt.system()),
                UserMessage.from(StruggleAnalysisPrompt.user(context, generateCount))
            )
            parseTasksFromJson(response.content().text(), context.moduleId)
        } else emptyList()

        val branchId = UUID.randomUUID().toString()
        deduplicationService.persistBranch(UUID.randomUUID().toString(), branchId, embedding)

        return AdaptiveBranch(
            branchId = branchId,
            subtasks = reusedTasks + freshTasks,
            reuseStrategy = ReuseStrategy.PARTIAL_REUSE,
            reusedFromBranchId = match.branchId
        )
    }

    private fun generateFreshBranch(context: StruggleContext, embedding: FloatArray): AdaptiveBranch {
        val systemMsg = SystemMessage.from(StruggleAnalysisPrompt.system())
        val userMsg = UserMessage.from(StruggleAnalysisPrompt.user(context, defaultSubtaskCount))
        var tasks = parseTasksFromJson(chatModel.generate(systemMsg, userMsg).content().text(), context.moduleId)
        if (tasks.none { it.status == GenerationStatus.SUCCESS }) {
            // AI returned tasks with no options or none at all — retry once before giving up.
            log.warn("No valid adaptive tasks on first attempt for task=${context.taskId} — retrying")
            meterRegistry.counter("ai.generation.failures", "type", "adaptive_schema_retry").increment()
            tasks = parseTasksFromJson(chatModel.generate(systemMsg, userMsg).content().text(), context.moduleId)
        }
        val branchId = UUID.randomUUID().toString()
        deduplicationService.persistBranch(UUID.randomUUID().toString(), branchId, embedding)
        return AdaptiveBranch(
            branchId = branchId,
            subtasks = tasks,
            reuseStrategy = ReuseStrategy.FRESH_GENERATION,
            reusedFromBranchId = null
        )
    }

    private fun parseTasksFromJson(
        rawJson: String,
        moduleId: String,
        onTaskParsed: ((completed: Int, total: Int) -> Unit)? = null,
        total: Int = 0
    ): List<GeneratedTask> {
        val json = Json { ignoreUnknownKeys = true }
        val cleaned = rawJson.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val array = json.parseToJsonElement(cleaned).jsonArray

        var successCount = 0
        return array.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val title = obj["title"]?.jsonPrimitive?.content ?: return@runCatching null
                val description = obj["description"]?.jsonPrimitive?.content ?: return@runCatching null
                val hint = obj["hint"]?.jsonPrimitive?.content ?: return@runCatching null
                val points = obj["pointsReward"]?.jsonPrimitive?.int ?: 20
                val optionsArray = obj["options"]?.jsonArray
                // A multiple-choice task with fewer than 2 options is unusable — skip it
                // before spending an embedding API call. This guards against schema
                // non-compliance where the AI omits or truncates the options field.
                if (optionsArray == null || optionsArray.size < 2) return@runCatching null
                val optionsJsonStr = optionsArray.toString()
                val correctIdx = obj["correctAnswerIndex"]?.jsonPrimitive?.int ?: 0

                // Embed for deduplication check
                val embedding = embeddingModel.embed("$title $description").content().vector()
                require(embedding.size == 1024) {
                    "Expected 1024-dimensional embedding from Mistral, got ${embedding.size}"
                }

                val isDuplicate = deduplicationService.isDuplicate(embedding, moduleId)
                val status = if (isDuplicate) GenerationStatus.DUPLICATE else GenerationStatus.SUCCESS
                if (status == GenerationStatus.SUCCESS) {
                    successCount++
                    onTaskParsed?.invoke(successCount, total)
                }

                GeneratedTask(
                    externalId = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    hint = hint,
                    pointsReward = points,
                    embedding = embedding,
                    status = status,
                    optionsJson = optionsJsonStr,
                    correctAnswerIndex = correctIdx
                )
            }.getOrNull()
        }
    }
}
