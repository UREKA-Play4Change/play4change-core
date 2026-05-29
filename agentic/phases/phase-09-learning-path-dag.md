# Phase 09 — Learning Path DAG

**Status:** `DONE`

## Goal

Add prerequisite graph (DAG) support so admins can define learning paths where certain topics
must be completed before others unlock. The graph is assembled post-creation — topics remain
independent and prerequisites are set by the admin at any time on ACTIVE topics.

## Architecture Decision

**Option A — join table, admin-driven:** Prerequisites stored in `topic_prerequisites` join table.
`Topic` aggregate unchanged. `PrerequisiteRepository` is the sole graph owner. BFS cycle
detection in service layer prevents cycles. `isLocked` computed server-side per-user so clients
never need to replicate badge/enrollment logic.

## Tasks

### Server

- [x] **9.0** `V23__create_topic_prerequisites.sql` — join table with FK CASCADE + unique constraint
- [x] **9.1** `PrerequisiteRepository` domain port (`findPrerequisitesByTopicId`, `setPrerequisites`, `findAllEdges`)
- [x] **9.2** `TopicPrerequisiteEntity` JPA entity + `TopicPrerequisiteJpaRepository`
- [x] **9.3** `PrerequisiteRepositoryAdapter` — `@Transactional` delete-all + re-insert
- [x] **9.4** `TopicUseCase` — `getPrerequisites`, `setPrerequisites`, `getLearningGraph` + data classes
- [x] **9.5** `TopicManagementService` — implement 3 methods with BFS cycle detection
- [x] **9.6** `EnrollmentRepository` / `EnrollmentRepositoryAdapter` — `findCompletedByUserId`
- [x] **9.7** `EnrollmentUseCase` / `EnrollmentService` — `getCompletedTopicIds`; prerequisite gate in `enroll()`
- [x] **9.8** `UserTopicResponse` — `isLocked: Boolean`, `prerequisiteTopicIds: List<String>`
- [x] **9.9** `UserTopicController` — compute `isLocked` per-user via `getCompletedTopicIds`
- [x] **9.10** DTOs: `SetPrerequisitesRequest`, `PrerequisiteTopicResponse`, `LearningGraphResponse`
- [x] **9.11** `AdminTopicPrerequisiteController` — `GET/POST /admin/topics/{id}/prerequisites`, `GET /admin/learning-graph`

### Admin Web (`play4change-web`)

- [x] **9.12** `Topic.ts` — `PrerequisiteTopic`, `LearningGraphEdge`, `LearningGraph`; `prerequisites?` on `Topic`
- [x] **9.13** `TopicPort.ts` — 3 new interface methods
- [x] **9.14** `TopicAdapter` + `MockTopicAdapter` — real + mock implementations
- [x] **9.15** `useTopics.ts` — `useTopicPrerequisites`, `useSetPrerequisites`, `useLearningGraph`
- [x] **9.16** `constants.ts` — `ADMIN_LEARNING_PATHS` route
- [x] **9.17** `PrerequisiteSelector.tsx` — checkbox UI, diff-aware save
- [x] **9.18** `LearningPathDagView.tsx` — graph renderer ("X requires [A] [B]")
- [x] **9.19** `LearningPathsPage.tsx` — stats row + full graph view
- [x] **9.20** `TopicDetailPage.tsx` — 5th "Prerequisites" tab
- [x] **9.21** `AdminLayout.tsx` — "Learning Paths" nav item
- [x] **9.22** `App.tsx` — lazy route `/admin/learning-paths`
- [x] **9.23** i18n — `admin.nav.learningPaths`, `admin.prerequisites.*`, `admin.learningPaths.*`, `common.cancel` in all 9 locales

### Mobile (`composeApp`)

- [x] **9.24** `Topic.kt` — `isLocked: Boolean = false`, `prerequisiteTopicIds: List<String> = emptyList()`
- [x] **9.25** `HttpExploreRepository.kt` — `UserTopicDto` picks up `isLocked`/`prerequisiteTopicIds`; `toTopic()` maps them
- [x] **9.26** `ExploreScreen.kt` — lock icon badge + disabled "Locked" button for locked topics

## Remaining

- [x] **9.27** Tests: `TopicPrerequisiteServiceTest`, `EnrollmentPrerequisiteGateTest`, `AdminTopicPrerequisiteControllerTest`
- [x] **9.28** Manual test recipe (human checkpoint)
- [x] **9.29** Merge PR + update phase status to DONE

## API Surface

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/admin/topics/{id}/prerequisites` | ADMIN | List prerequisite topics |
| `POST` | `/admin/topics/{id}/prerequisites` | ADMIN | Replace prerequisite set (cycle check) |
| `GET` | `/admin/learning-graph` | ADMIN | Full DAG (all edges) |
| `GET` | `/topics` | USER | Now includes `isLocked`, `prerequisiteTopicIds` |

## Human Checkpoint

1. In admin web, open any two ACTIVE topics A and B.
2. Open B → Prerequisites tab → check A → Save.
3. On mobile, log in as a user who has not completed A. Browse topics — B shows a lock icon.
4. Tap B's enroll button — server returns 400 (prerequisites not completed).
5. Complete all of A's tasks. B should now show the normal enroll button.
6. Enroll in B successfully.
7. In admin web, go to Learning Paths — the A→B edge appears in the graph.
8. Try to set A's prerequisite to B (would create A↔B cycle) — server returns 400.
