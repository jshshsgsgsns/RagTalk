# Memory Backend 演示项目

## 项目简介

Memory Backend 是一个基于 Spring Boot 3 的长期记忆后端演示项目。
它将以下能力组合在一起：

- 使用 SQLite 保存权威业务数据
- 使用 Spring AI 提供聊天模型和向量模型抽象
- 通过 Spring AI `VectorStore` 接入 Qdrant
- 支持规则版记忆提取、向量检索与 RAG 对话

这个项目遵循一个核心原则：

业务历史保存在 SQLite，AI 记忆和向量检索能力建立在业务数据之上。

`message_event` 是完整消息与事件归档表。
Spring AI `ChatMemory` 不作为完整历史的权威存储。

## 技术栈

- Java 17
- Spring Boot 3.4.3
- Spring AI 1.0.0
- MyBatis-Plus 3.5.15
- SQLite (`sqlite-jdbc`)
- Qdrant Java Client 1.13.0
- Spring AI Qdrant Vector Store
- Lombok
- JUnit 5 + Spring Boot Test + MockMvc

## 目录结构

```text
src/main/java/com/example/memory
├─ config        # AI、向量库、聊天、提取、RAG 配置
├─ controller    # REST 接口
├─ dto           # 请求对象
├─ entity        # SQLite 实体
├─ mapper        # MyBatis-Plus Mapper
├─ service       # 编排、记忆、画像、时间线、向量、RAG 逻辑
├─ vo            # 返回对象
└─ common        # 统一返回结构和状态码

src/main/resources
├─ application.yml
└─ schema.sql

src/test/java
└─ 基于 Spring Boot 的集成风格测试
```

## Spring AI 使用说明

项目使用了 Spring AI 官方抽象：

- `ChatModel` 和 `ChatClient` 用于聊天生成
- `EmbeddingModel` 用于向量生成
- `VectorStore` 用于向量存储与检索

当前主要 Bean：

- 聊天模型 Bean：`chatModel`
- 聊天客户端 Bean：`chatClient`
- 向量模型 Bean：`embeddingModel`
- 向量存储 Bean：`memoryVectorStore`

关键配置/实现类：

- `AiModelConfiguration`
- `VectorStoreConfiguration`
- `ChatOrchestrationService`
- `RagChatService`
- `VectorMemoryService`

## 如何配置聊天模型

聊天模型配置位于 `application.yml`：

```yaml
app:
  ai:
    chat:
      provider: ollama
      model: qwen2.5:7b
      temperature: 0.2
      max-tokens: 1024
```

供应商默认配置：

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

## 如何配置 Embedding 模型

向量模型配置位于：

```yaml
app:
  ai:
    embedding:
      provider: ollama
      model: nomic-embed-text
```

如果切换到 OpenAI，常见配置如下：

- 聊天模型：`gpt-4o-mini`
- embedding 模型：`text-embedding-3-small`

## 如何配置 Qdrant

Qdrant 配置位于：

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

说明：

- `enabled: false` 表示本地没有 Qdrant 时也能直接启动项目
- 启用后，项目通过 Spring AI `QdrantVectorStore` 接入 Qdrant
- 只有高价值 `memory_record` 会写入向量库
- `message_event` 不会被全量向量化

## SQLite 数据说明

数据库结构通过 `src/main/resources/schema.sql` 自动初始化。

核心表：

- `user_account`
- `project_space`
- `chat_session`
- `message_event`
- `memory_record`
- `conversation_summary`

规则：

- `message_event` 保存完整用户/AI 消息和事件流
- `memory_record` 保存提炼后的高价值记忆
- `conversation_summary` 保存滚动摘要
- 时间字段统一使用 UTC 毫秒时间戳（`INTEGER`）

如果需要重置本地数据库：

```powershell
Remove-Item .\memory-backend.db
```

下次启动时会自动重新建表。

## 项目隔离说明

项目隔离同时体现在聊天层和检索层。

- `chat_session` 必须属于当前 `project_space`
- 不允许跨项目写消息
- RAG 检索只会使用：
  - 当前项目下的 `project` 记忆
  - 当前聊天下的 `chat` 记忆
  - `global` 范围内的 `preference/profile/habit` 作为补充
- 旧项目里的 `requirement / constraint / decision` 不会混入新项目对话

这保证了：

- 当前项目上下文严格隔离
- 用户全局偏好可安全复用
- 旧项目约束不会污染新项目

## 记忆提取说明

当前记忆提取采用规则版实现，后续可平滑替换为 LLM 提取器。

scope 取值：

- `global`
- `project`
- `chat`

memory_type 取值：

- `preference`
- `profile`
- `habit`
- `requirement`
- `constraint`
- `decision`
- `summary`
- `fact`

当前规则示例：

- `喜欢`、`偏好`、`习惯`、`以后` -> preference/profile/habit 家族
- `需要`、`必须`、`不允许`、`要求` -> requirement/constraint 家族
- `决定`、`最终`、`确定` -> decision

## 主要接口说明

### 健康检查

- `GET /api/health`

### AI 冒烟测试

- `POST /api/ai/chat/test`
- `POST /api/ai/embedding/test`

### 聊天接口

- `POST /api/chat/send`
- `POST /api/chat/rag-send`

### 记忆接口

- `POST /api/memory-records`
- `GET /api/memory-records/{memoryId}`
- `GET /api/memory-records`
- `POST /api/chat/{chatId}/extract-memory`

### 向量接口

- `POST /api/vector/memory/upsert/{memoryId}`
- `POST /api/vector/memory/search`

### 用户洞察接口

- `GET /api/profile/{userId}`
- `GET /api/timeline/{userId}`

## 最小演示流程

项目默认不会自动插入业务演示数据。
当前方式是：

- 自动初始化表结构
- 通过测试或接口手动构造演示数据

建议演示顺序：

1. 启动项目
2. 通过 SQLite 或 SQL 工具插入用户、项目、会话基础数据
3. 使用 `/api/chat/send` 发送消息
4. 使用 `/api/chat/{chatId}/extract-memory` 提取记忆
5. 使用 `/api/vector/memory/upsert/{memoryId}` 将记忆写入向量库
6. 使用 `/api/chat/rag-send` 演示检索增强对话
7. 使用 `/api/profile/{userId}` 和 `/api/timeline/{userId}` 演示用户画像与时间线

`MemoryBackendApplicationTests` 里的集成测试也可以直接作为演示参考。

## 如何从本地模型切换到云模型

### 本地 Ollama

默认示例：

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

切换到云模型时，只需要改配置，不需要改业务代码：

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

## 启动方式

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

运行测试：

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd test
```

## 当前测试覆盖

当前测试已经覆盖：

- 消息发送流程
- 项目隔离校验
- 记忆提取
- 向量写入
- 向量检索
- RAG 对话
- 用户画像聚合
- 用户时间线聚合

## 当前项目状态

这个项目已经达到演示级可用状态：

- SQLite 保存完整业务历史
- `memory_record` 保存高价值长期记忆
- 高价值记忆通过 Spring AI `VectorStore` 写入 Qdrant
- RAG 对话具备明确的项目隔离策略
- 支持用户画像和用户时间线展示

下一步比较自然的演进方向：

- 在保持当前存储和向量层接口不变的前提下，将规则版记忆提取器替换为 LLM 版提取器
