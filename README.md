
# 睿码AI中心 

## 项目简介

睿码AI中心是一个AI研发流程管理平台，集成AI代码评审（AICR）、AI单元测试生成、团队智能问答知识库等功能。

## 技术栈

- **框架**: Spring Boot 2.7.18 + Java 17
- **LLM框架**: Langchain4j 0.30.0
- **对话模型**: DeepSeek-Chat (OpenAI 兼容接口)
- **向量模型**: Qwen text-embedding-v3 (DashScope)
- **向量数据库**: Chroma（本地 Python 进程）+ InMemoryEmbeddingStore（自动回退）
- **文档解析**: Apache Tika
- **数据库**: MySQL 8.0 + JPA (Hibernate)
- **记忆存储**: Redis (Redisson) + RediSearch
- **前端**: Vue 3 + Vite + Element Plus（浅绿+白色主题）
- **测试**: JUnit 5 + Mockito 4.5.1

## 项目结构

```
src/main/java/com/ruima/ai/center/
├── RuimaAiCenterApplication.java    # 启动类
├── config/                           # 配置类
│   ├── Langchain4jConfig.java       # DeepSeek Chat + DashScope Embedding
│   ├── EmbeddingStoreConfig.java    # Chroma 向量库（自动回退 InMemory）
│   ├── ChromaV2Client.java          # Chroma v2 API 适配器
│   └── RedisConfig.java             # Redis/会话记忆配置
├── controller/                       # REST控制器
│   ├── AiCodeReviewController.java  # AICR + 评审历史
│   ├── KnowledgeBaseController.java # 知识库 + 文档管理
│   ├── MemoryController.java        # 短期/长期记忆
│   ├── UnitTestController.java      # 单元测试生成+覆盖率
│   └── AuthController.java          # 注册/登录/改密码
├── service/                          # 服务接口
│   ├── AiCodeReviewService.java
│   ├── MemoryService.java
│   ├── RagService.java              # RAG + 文档管理
│   ├── KnowledgeBaseService.java
│   ├── UnitTestService.java
│   ├── AuthService.java
│   └── impl/                         # 服务实现
├── repository/
│   └── UserRepository.java          # 用户 JPA 仓库
├── model/
│   ├── entity/User.java             # 用户实体
│   ├── dto/                          # 数据传输对象
│   └── enums/                        # 枚举类
└── util/                             # 工具类
    ├── PromptTemplate.java           # AICR + 单元测试提示词
    └── TextChunker.java              # 语义分块

frontend/src/
├── views/
│   ├── Login.vue                    # 登录/注册（弹窗注册）
│   ├── CodeReview.vue               # AICR（文件/粘贴+评审历史+导出MD）
│   ├── KnowledgeBase.vue            # 知识库（问答+文档列表+历史会话）
│   ├── UnitTest.vue                 # 单元测试（文件/粘贴+覆盖率分析）
│   └── MemoryManage.vue             # 记忆管理（会话下拉+长期记忆）
├── components/
│   └── AppLayout.vue                # 侧边栏（含改密码入口）
├── stores/
│   └── auth.js                      # 认证状态管理
├── api/
│   └── index.js                     # 后端 API 封装
└── styles/
    └── global.css                   # 浅绿+白色主题
```

## 核心模块

### 1. AI Code Review (AICR)
- Prompt：精简结构化提示词，空指针/安全/资源泄漏等必检项优先
- 前端：文件/文件夹上传 + 粘贴代码双模式，8维全选默认
- 后端解析：按"问题类型:"分割，关键词自动升级严重级别
- 评审历史：Redis SortedSet 存储，支持回溯和删除
- 报告导出：一键下载 Markdown

### 2. 团队智能问答知识库
- 三合一上下文：短期记忆(会话) + 长期记忆(个性化) + RAG检索
- 文档管理：上传/列表/删除，元数据存 Redis Hash
- 检索策略：Chroma 向量检索 + BM25 混合
- 会话管理：localStorage 持久化 userId，历史会话下拉选择

### 3. 记忆管理
- **短期记忆**：Redis SortedSet，滑动窗口 + Token 摘要压缩(32K阈值)
- **长期记忆**：Chroma 向量存储 + Redis Hash 元数据(30天TTL)
- **用户关联**：`ruima:user:sessions:{userId}` Set

### 4. AI 单元测试
- 生成：文件/文件夹 + 粘贴代码 → JUnit 5 + Mockito
- 覆盖率：行/分支/方法/异常 四项指标 + 评级

## API 端点

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| AICR | POST | /api/aicr/review | 代码评审（JSON） |
| AICR | POST | /api/aicr/review/files | 代码评审（文件上传） |
| AICR | GET | /api/aicr/history?userId= | 评审历史列表 |
| AICR | DELETE | /api/aicr/history/{id}?userId= | 删除评审历史 |
| AICR | GET | /api/aicr/health | 健康检查 |
| 知识库 | POST | /api/kb/ask | 智能问答 |
| 知识库 | GET | /api/kb/context/{sessionId} | 会话上下文 |
| 知识库 | GET | /api/kb/sessions?userId= | 用户会话列表 |
| 知识库 | POST | /api/kb/documents/upload | 上传文档（JSON） |
| 知识库 | POST | /api/kb/documents/upload/file | 上传文档（文件） |
| 知识库 | GET | /api/kb/documents | 文档列表 |
| 知识库 | DELETE | /api/kb/documents/{id} | 删除文档 |
| 知识库 | POST | /api/kb/search | 搜索知识库 |
| 记忆 | GET | /api/memory/history/{sessionId} | 对话历史 |
| 记忆 | POST | /api/memory/message | 添加消息 |
| 记忆 | POST | /api/memory/summary/{sessionId} | 生成摘要 |
| 记忆 | POST | /api/memory/long-term/store | 存储长期记忆 |
| 记忆 | POST | /api/memory/long-term/recall | 召回长期记忆 |
| 测试 | POST | /api/test/generate | 生成测试（JSON） |
| 测试 | POST | /api/test/generate/file | 生成测试（文件） |
| 测试 | POST | /api/test/coverage/analyze | 分析覆盖率（JSON） |
| 测试 | POST | /api/test/coverage/analyze/file | 分析覆盖率（文件） |
| 测试 | POST | /api/test/coverage/report | 生成覆盖率报告 |
| 认证 | POST | /api/auth/register | 注册 |
| 认证 | POST | /api/auth/login | 登录 |
| 认证 | PUT | /api/auth/password | 修改密码 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DEEPSEEK_API_KEY | DeepSeek API 密钥 | - |
| DASHSCOPE_API_KEY | DashScope API 密钥（Embedding） | - |
| REDIS_HOST | Redis 主机 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| CHROMA_URL | Chroma 服务地址 | http://localhost:8000 |
