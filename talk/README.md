# Memory Backend Demo

## Overview

Memory Backend is a Spring Boot 3 demo backend for long-term conversational memory.
It combines:

- SQLite for authoritative business data
- Spring AI for chat and embedding abstractions
- Qdrant through Spring AI `VectorStore`
- Rule-based memory extraction, vector retrieval, and RAG chat

The project is designed around one core principle:

Business history lives in SQLite. AI memory and vector retrieval are layered on top of it.

`message_event` is the complete message/event archive.
Spring AI `ChatMemory` is not used as the source of truth for full history.

## Tech Stack

- Java 17
- Spring Boot 3.4.3
- Spring AI 1.0.0
- MyBatis-Plus 3.5.15
- SQLite (`sqlite-jdbc`)
- Qdrant Java Client 1.13.0
- Spring AI Qdrant Vector Store
- Lombok
- JUnit 5 + Spring Boot Test + MockMvc

## Project Structure

```text
src/main/java/com/example/memory
├─ config        # AI, vector store, chat, extraction, RAG configuration
├─ controller    # REST endpoints
├─ dto           # request objects
├─ entity        # SQLite entities
├─ mapper        # MyBatis-Plus mappers
├─ service       # orchestration, memory, profile, timeline, vector, RAG logic
├─ vo            # response objects
└─ common        # unified response wrapper and codes

src/main/resources
├─ application.yml
└─ schema.sql

src/test/java
└─ integration-style Spring Boot tests
```

## Spring AI Usage

The project uses Spring AI official abstractions:

- `ChatModel` and `ChatClient` for chat generation
- `EmbeddingModel` for embedding generation
- `VectorStore` for vector operations

Current wiring:

- chat model bean: `chatModel`
- chat client bean: `chatClient`
- embedding model bean: `embeddingModel`
- vector store bean: `memoryVectorStore`

Key classes:

- `AiModelConfiguration`
- `VectorStoreConfiguration`
- `ChatOrchestrationService`
- `RagChatService`
- `VectorMemoryService`

## Chat Model Configuration

Chat model settings live in `application.yml` under:

```yaml
app:
  ai:
    chat:
      provider: ollama
      model: qwen2.5:7b
      temperature: 0.2
      max-tokens: 1024
```

Provider defaults:

```yaml
app:
  ai:
    providers:
      ollama:
        base-url: http://localhost:11434
        api-key: ollama
        completions-path: /v1/chat/completions
      openai:
        base-url: https://api.openai.com
        api-key: ${OPENAI_API_KEY:}
        completions-path: /v1/chat/completions
```

## Embedding Model Configuration

Embedding model settings live under:

```yaml
app:
  ai:
    embedding:
      provider: ollama
      model: nomic-embed-text
```

For OpenAI, typical values are:

- chat model: `gpt-4o-mini`
- embedding model: `text-embedding-3-small`

## Qdrant Configuration

Qdrant is configured under:

```yaml
app:
  vector:
    qdrant:
      enabled: false
      host: localhost
      grpc-port: 6334
      use-tls: false
      api-key:
      collection-name: memory_record_vectors
      initialize-schema: true
      search-top-k-default: 5
      timeout-seconds: 5
```

Notes:

- `enabled: false` keeps local startup simple without a running Qdrant instance.
- When enabled, the project uses Spring AI `QdrantVectorStore`.
- Only high-value `memory_record` entries are written to the vector store.
- `message_event` is never bulk-vectorized.

## SQLite Data Model

Schema is initialized from `src/main/resources/schema.sql`.

Main tables:

- `user_account`
- `project_space`
- `chat_session`
- `message_event`
- `memory_record`
- `conversation_summary`

Rules:

- `message_event` stores full user/assistant message history and event payloads
- `memory_record` stores extracted high-value memory
- `conversation_summary` stores rolling summaries
- time fields use UTC epoch millis (`INTEGER`)

Reset local database:

```powershell
Remove-Item .\memory-backend.db
```

On next startup, schema will be recreated automatically.

## Project Isolation Rules

Project isolation is enforced in both chat and retrieval layers.

- A `chat_session` must belong to the current `project_space`
- Cross-project message writes are rejected
- RAG retrieval only uses:
  - current project `project` memories
  - current chat `chat` memories
  - `global` preference/profile/habit memories as supplement
- Old project `requirement` / `constraint` / `decision` memories are not mixed into new project RAG prompts

This allows:

- current project context to stay strict
- global user preferences to be reused safely
- old project constraints to stay isolated

## Memory Extraction

Current memory extraction is rule-based and easy to replace later with an LLM extractor.

Scope values:

- `global`
- `project`
- `chat`

Memory type values:

- `preference`
- `profile`
- `habit`
- `requirement`
- `constraint`
- `decision`
- `summary`
- `fact`

Current rule examples:

- keywords like `喜欢`, `偏好`, `习惯`, `以后` -> preference/profile/habit family
- keywords like `需要`, `必须`, `不允许`, `要求` -> requirement/constraint family
- keywords like `决定`, `最终`, `确定` -> decision

## Main APIs

### Health

- `GET /api/health`

### AI Smoke Test

- `POST /api/ai/chat/test`
- `POST /api/ai/embedding/test`

### Chat

- `POST /api/chat/send`
- `POST /api/chat/rag-send`

### Memory

- `POST /api/memory-records`
- `GET /api/memory-records/{memoryId}`
- `GET /api/memory-records`
- `POST /api/chat/{chatId}/extract-memory`

### Vector

- `POST /api/vector/memory/upsert/{memoryId}`
- `POST /api/vector/memory/search`

### User Insight

- `GET /api/profile/{userId}`
- `GET /api/timeline/{userId}`

## Minimal Demo Flow

This project does not auto-load business demo data by default.
Instead, schema initializes automatically and you can create demo data through APIs or tests.

Recommended demo sequence:

1. Start the app
2. Create a user/project/chat row with SQLite or a SQL client
3. Send messages with `/api/chat/send`
4. Extract memory with `/api/chat/{chatId}/extract-memory`
5. Upsert a memory to vector store with `/api/vector/memory/upsert/{memoryId}`
6. Use `/api/chat/rag-send` to demonstrate retrieval-augmented chat
7. View `/api/profile/{userId}` and `/api/timeline/{userId}`

Dedicated tests now carry the business-flow examples. `MemoryBackendApplicationTests` is intentionally kept as a minimal bootstrap smoke test only.

## Switching Local Models to Cloud Models

### Local Ollama

Default example:

```yaml
app:
  ai:
    chat:
      provider: ollama
      model: qwen2.5:7b
    embedding:
      provider: ollama
      model: nomic-embed-text
```

### OpenAI

Switch to cloud models by changing only configuration:

```yaml
app:
  ai:
    chat:
      provider: openai
      model: gpt-4o-mini
    embedding:
      provider: openai
      model: text-embedding-3-small
    providers:
      openai:
        api-key: ${OPENAI_API_KEY}
```

No business service code changes are required.

## Run

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Run tests:

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd test
```

## Testing Strategy

The test suite is split by responsibility:

- `MemoryBackendApplicationTests`
  - minimal smoke coverage only
  - verifies application startup, schema bootstrap, and `/api/health`
- dedicated controller/service/integration tests
  - carry business assertions and edge cases
  - examples: chat flow, RAG flow, memory extraction, memory dedup, validation, conversation summary, profile, timeline, and configuration wiring
- configuration-focused tests
  - verify `qdrant.enabled=false` startup behavior
  - verify `VectorStore`, `ChatModel`, `ChatClient`, and `EmbeddingModel` wiring without real external services

This keeps the old broad smoke test from turning into a second integration suite.

Current dedicated coverage includes:

- message send flow
- project isolation validation
- memory extraction happy path and edge cases
- memory dedup and update behavior
- vector upsert and vector search
- RAG chat
- controller validation and error responses
- conversation summary generation and timeline integration
- user profile aggregation
- unified timeline aggregation
- configuration and bean wiring

## Current Status

This project is ready for demo-level use:

- business history stored in SQLite
- long-term memory extracted into `memory_record`
- high-value memory synced into Qdrant through Spring AI `VectorStore`
- RAG chat with explicit project isolation
- user profile and user timeline aggregation

Next likely step:

- replace rule-based memory extraction with an LLM-based extractor while keeping the current storage and vector interfaces unchanged
