# Test Overview

This document records the current automated test suite for the Spring Boot + Spring AI long-term memory demo backend.

The goal is to keep test responsibilities explicit:

- `MemoryBackendApplicationTests`
  - minimal smoke test only
  - startup, schema bootstrap, and health endpoint
- dedicated controller/service/integration tests
  - carry real business assertions, isolation semantics, validation, and update paths
- configuration tests
  - verify bean wiring and `qdrant.enabled=false` behavior without real external services

## Current Test Classes

### Smoke tests

- `src/test/java/com/example/memory/MemoryBackendApplicationTests.java`
  - verifies Spring Boot application startup
  - verifies schema bootstrap creates required tables and indexes
  - verifies `/api/health` returns a successful unified response
  - intentionally does not carry business-flow assertions

- `src/test/java/com/example/memory/ApplicationBootstrapSmokeTest.java`
  - verifies application can start with `app.vector.qdrant.enabled=false`
  - verifies `memoryVectorStore`, `ChatModel`, `ChatClient`, and `EmbeddingModel` are all present
  - verifies `QdrantGrpcClient` and `QdrantClient` are absent when Qdrant is disabled
  - uses `/api/health` as a minimal runtime bootstrap check

### Configuration tests

- `src/test/java/com/example/memory/config/VectorStoreConfigurationTest.java`
  - verifies `memoryVectorStore` falls back to a no-op implementation when Qdrant is disabled
  - verifies no Qdrant client beans are created in disabled mode
  - verifies a stub `VectorStore` can be injected into `VectorMemoryService`

- `src/test/java/com/example/memory/config/AiModelConfigurationTest.java`
  - verifies `ChatModel` and `ChatClient` wiring when chat provider config is present
  - verifies `EmbeddingModel` wiring when embedding provider config is present
  - does not depend on real model services

### Controller integration and validation tests

- `src/test/java/com/example/memory/controller/ChatControllerIntegrationTest.java`
  - covers `/api/chat/send`
  - verifies normal message send flow
  - verifies user and assistant `message_event` persistence
  - verifies `sequence_no` increments correctly
  - verifies `chat_session.updated_at` and `last_message_at` updates
  - verifies project isolation and cross-project write rejection

- `src/test/java/com/example/memory/controller/RagChatIntegrationTest.java`
  - covers `/api/chat/rag-send`
  - verifies full RAG flow with retrieved project/chat/global memories
  - verifies fallback behavior when retrieval returns empty results
  - verifies current-project isolation through vector search filters
  - verifies user and assistant messages are persisted in RAG flow

- `src/test/java/com/example/memory/controller/ProfileTimelineIntegrationTest.java`
  - covers `/api/profile/{userId}`
  - covers `/api/timeline/{userId}`
  - verifies `global` memories are aggregated into structured profile buckets
  - verifies timeline merges `PROJECT_SPACE`, `CHAT_SESSION`, `MESSAGE_EVENT`, and `MEMORY_RECORD`
  - verifies reverse chronological ordering
  - verifies unrelated user data is excluded

- `src/test/java/com/example/memory/controller/ConversationSummaryIntegrationTest.java`
  - verifies generated `conversation_summary` records appear in user timeline output
  - verifies summary events are exposed as `CONVERSATION_SUMMARY`
  - verifies summary event title/detail fields match stored summary metadata

- `src/test/java/com/example/memory/controller/MemoryRecordControllerValidationTest.java`
  - covers invalid create/list/detail requests for `/api/memory-records`
  - verifies missing required fields
  - verifies invalid `memoryType`
  - verifies invalid `scopeType`
  - verifies nonexistent `memoryId`
  - verifies unified error response `code/message`

- `src/test/java/com/example/memory/controller/VectorMemoryControllerValidationTest.java`
  - covers invalid requests for `/api/vector/memory`
  - verifies blank `queryText`
  - verifies `topK <= 0`
  - verifies invalid `scopeTypes`
  - verifies nonexistent `memoryId` on upsert
  - verifies unified error response `code/message`

- `src/test/java/com/example/memory/controller/AiTestControllerValidationTest.java`
  - covers invalid requests for `/api/ai/chat/test` and `/api/ai/embedding/test`
  - verifies blank `message`
  - verifies blank `text`
  - verifies unified error response `code/message`

### Service tests

- `src/test/java/com/example/memory/service/MemoryExtractionServiceTest.java`
  - covers `MemoryExtractionService.extractFromChat`
  - verifies extraction of:
    - `profile`
    - `preference`
    - `habit`
    - `requirement`
    - `constraint`
    - `decision`
  - verifies extracted records are persisted into `memory_record`
  - verifies vector sync is triggered through mocked `VectorMemoryService`

- `src/test/java/com/example/memory/service/MemoryExtractionServiceEdgeCaseTest.java`
  - verifies `chat session not found`
  - verifies chat/project mismatch rejection
  - verifies no-message chat returns safely
  - verifies assistant-only messages are ignored
  - verifies blank user messages are skipped
  - verifies non-extractable content returns empty result safely

- `src/test/java/com/example/memory/service/MemoryRecordServiceDedupTest.java`
  - covers `MemoryRecordService.saveOrUpdateExtractedMemory`
  - verifies first insert path
  - verifies same `memoryKey` updates instead of inserting a duplicate
  - verifies project-scoped records do not merge across different projects
  - verifies same key does not merge across different scopes
  - verifies vector sync is triggered on both insert and update

- `src/test/java/com/example/memory/service/VectorMemoryServiceTest.java`
  - covers `VectorMemoryService`
  - verifies high-value memory upsert converts `MemoryRecord` into vector `Document`
  - verifies vector add invocation
  - verifies staged search result merging across `chat`, `project`, and supplemental `global`
  - verifies search filter construction for project isolation and RAG lookup
  - verifies empty-result behavior under mocked vector store

- `src/test/java/com/example/memory/service/ConversationSummaryServiceTest.java`
  - covers `ConversationSummaryService.generateSummary`
  - verifies summary generation for `chatId + message range`
  - verifies summary persistence into `conversation_summary`
  - verifies same range updates existing summary instead of appending
  - verifies different ranges append new `summaryVersion`

## Database and Dependency Strategy

### SQLite

- Spring Boot integration tests use real SQLite database files under `target/`
- tests seed data directly with `JdbcTemplate`
- assertions inspect real persisted rows instead of only checking HTTP status

### AI and vector dependencies

- no real model calls are used in automated tests
- `ChatClient` is mocked in chat and RAG integration tests
- `VectorStore` is mocked or stubbed in vector-related tests
- Qdrant is disabled in Spring Boot integration tests unless a configuration test is explicitly checking bean conditions

## Responsibility Split

### Smoke tests should answer only these questions

- can the application context start
- is schema initialization intact
- is the basic health endpoint alive

### Dedicated tests should answer these questions

- does the business flow persist correct records
- are project and chat isolation boundaries enforced
- are validation and error responses stable
- do dedup/update rules prevent dirty memory growth
- does summary generation form a verifiable closed loop
- does configuration wiring stay stable across provider toggles

## Recommended Commands

### Run all tests

```powershell
& 'C:\Users\jiang\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd' test
```

### Run smoke tests only

```powershell
& 'C:\Users\jiang\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd' '-Dtest=MemoryBackendApplicationTests,ApplicationBootstrapSmokeTest' test
```

### Run configuration tests only

```powershell
& 'C:\Users\jiang\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd' '-Dtest=VectorStoreConfigurationTest,AiModelConfigurationTest,ApplicationBootstrapSmokeTest' test
```

### Run key dedicated business tests

```powershell
& 'C:\Users\jiang\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd' '-Dtest=ChatControllerIntegrationTest,RagChatIntegrationTest,MemoryExtractionServiceTest,MemoryExtractionServiceEdgeCaseTest,MemoryRecordServiceDedupTest,ConversationSummaryServiceTest,ConversationSummaryIntegrationTest' test
```

## Current Status

- current test suite is split between smoke, dedicated business tests, and configuration tests
- `MemoryBackendApplicationTests` has already been reduced to a minimal smoke role
- business scenarios should continue to be added to dedicated test classes instead of being pushed back into the smoke test
