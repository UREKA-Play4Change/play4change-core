package com.ureka.play4change.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ureka.play4change.features.task.data.TaskCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager periodic worker that pre-fetches today's task and roadmap data for each enrolled
 * topic, storing results in [TaskCache] so the app opens without a visible loading spinner.
 *
 * Constraints (set in [WorkManagerSetup]):
 * - [NetworkType.UNMETERED] — WiFi only.
 * - [requiresBatteryNotLow] — skipped when battery is critically low.
 *
 * Work tag: [WORK_TAG] — used to cancel and re-enqueue when enrolled topics change.
 *
 * On network failure the worker returns [Result.retry()] and WorkManager applies exponential backoff.
 */
class BackgroundFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val client: HttpClient by inject()
    private val cache: TaskCache by inject()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result = runCatching {
        val topics = fetchEnrolledTopics()
        coroutineScope {
            topics.map { topic ->
                async {
                    preFetchTask(topic.id)
                    preFetchRoadmap(topic.id)
                }
            }.awaitAll()
        }
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    private suspend fun fetchEnrolledTopics(): List<TopicSummaryDto> {
        val response = client.get("topics")
        val all = json.decodeFromString<List<TopicSummaryDto>>(response.bodyAsText())
        return all.filter { it.isEnrolled }
    }

    private suspend fun preFetchTask(topicId: String) {
        val key = "tasks/today?topicId=$topicId"
        cache.getOrFetch(key) {
            client.get("tasks/today") {
                parameter("topicId", topicId)
                parameter("X-Timezone", TimeZone.currentSystemDefault().id)
            }.bodyAsText()
        }
    }

    private suspend fun preFetchRoadmap(topicId: String) {
        val key = "topics/$topicId/roadmap"
        cache.getOrFetch(key) {
            client.get("topics/$topicId/roadmap").bodyAsText()
        }
    }

    @Serializable
    private data class TopicSummaryDto(
        val id: String,
        val isEnrolled: Boolean = false
    )

    companion object {
        const val WORK_TAG = "background-task-fetch"
    }
}
