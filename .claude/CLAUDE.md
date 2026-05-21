
# 睿码AI中心 - 项目级 CLAUDE.md

## 项目简介

睿码AI中心是一个AI研发流程管理平台，集成AI代码评审（AICR）、AI单元测试生成、团队智能问答知识库等功能。

## 技术栈

- **框架**: Spring Boot 2.7.18 + Java 17
- **LLM框架**: Langchain4j 0.30.0
- **大模型**: Qwen-plus (通过DashScope API)
- **向量模型**: text-embedding-v3
- **向量数据库**: InMemoryEmbeddingStore（内置）
- **文档解析**: Apache Tika
- **记忆存储**: Redis (Redisson)
- **前端**: Vue 3 + Vite + Element Plus（浅蓝+白色主题）
- **测试**: JUnit 5 + Mockito 4.5.1

## 项目结构

```
src/main/java/com/ruima/ai/center/
├── RuimaAiCenterApplication.java    # 启动类
├── config/                           # 配置类
│   ├── Langchain4jConfig.java       # LLM/Embedding配置
│   ├── EmbeddingStoreConfig.java    # 向量存储配置
│   └── RedisConfig.java             # Redis/会话记忆配置
├── controller/                       # REST控制器
│   ├── AiCodeReviewController.java  # AI代码评审
│   ├── KnowledgeBaseController.java # 智能问答知识库
│   ├── MemoryController.java        # 记忆管理
│   ├── UnitTestController.java      # 单元测试生成
├── service/                          # 服务接口
│   ├── AiCodeReviewService.java
│   ├── MemoryService.java
│   ├── RagService.java
│   ├── KnowledgeBaseService.java
│   ├── UnitTestService.java
│   └── impl/                         # 服务实现
├── model/
│   ├── dto/                          # 数据传输对象
│   │   ├── ChatMessage.java         # 消息实体（role/content/sessionId/userId/timestamp）
│   │   ├── CodeReviewReport.java    # 评审报告
│   │   ├── CodeReviewIssue.java     # 评审问题
│   │   └── ...
│   └── enums/                        # 枚举类
└── util/                             # 工具类
    ├── PromptTemplate.java           # 结构化提示词模板
    └── TextChunker.java              # 语义分块工具

frontend/src/
├── views/
│   ├── CodeReview.vue               # AI代码评审（文件/文件夹上传 + 评审报告）
│   ├── KnowledgeBase.vue            # 智能问答（对话 + 文档上传 + 历史会话）
│   ├── UnitTest.vue                 # 单元测试（生成 + 覆盖率分析，支持文件夹）
│   └── MemoryManage.vue             # 记忆管理（短期/长期记忆可视化）
├── components/
│   └── AppLayout.vue                # 侧边栏布局（白色+浅蓝主题）
├── api/
│   └── index.js                     # 所有后端API封装
└── styles/
    └── global.css                   # 全局浅蓝+白色主题变量
```

## 核心模块

### 1. AI Code Review (AICR)
- 前端：文件/文件夹拖拽上传 → 勾选评审维度 → 点击评审
- 后端：Tika解析文件内容 → 结构化Prompt → LLM评审 → 解析JSON报告
- 三级问题分类：Critical(必须修复) / Warning(建议修复) / Info(优化建议)
- 8项评审维度：代码质量、安全性、可维护性、架构设计、Java特定、数据库、测试、性能

### 2. 团队智能问答知识库
- 对话流程：用户问题 → 短期记忆(会话上下文) + 长期记忆(个性化知识) + RAG检索 → LLM生成回答
- 会话管理：localStorage持久化userId → 用户-会话关联(Redis Set) → 历史会话列表按用户过滤
- 文档上传：Tika解析 → 语义分块 → InMemoryEmbeddingStore向量化
- 检索策略：混合检索（向量70% + BM25 30%）

### 3. 记忆管理
- **短期记忆**：Redis SortedSet（时间戳排序），滑动窗口保留最近10条
  - 摘要压缩：token超过32K阈值自动触发，LLM将旧消息压缩为系统摘要
  - 用户关联：`ruima:user:sessions:{userId}` Set 记录用户-会话映射
- **长期记忆**：InMemoryEmbeddingStore存向量 + Redis Hash存元数据
  - 存储：文本向量化 → 写入向量库 + Redis元数据（内容/分类/时间戳/30天TTL）
  - 召回：查询向量化 → 语义相似度搜索（min 0.75, top-5）


### 4. AI单元测试
- 生成：上传源码文件/文件夹 → Tika解析 → LLM生成JUnit 5 + Mockito测试代码
- 覆盖率：上传源码+测试代码 → LLM分析行/分支/方法/异常覆盖率 → 评级+风险评估
- 前端支持文件和文件夹两种上传方式

## API端点

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| AICR | POST | /api/aicr/review | 执行代码评审（JSON） |
| AICR | POST | /api/aicr/review/files | 执行代码评审（文件上传） |
| AICR | GET | /api/aicr/health | 服务健康检查 |
| 知识库 | POST | /api/kb/ask | 智能问答 |
| 知识库 | GET | /api/kb/context/{sessionId} | 获取会话上下文 |
| 知识库 | GET | /api/kb/sessions?userId= | 获取用户历史会话列表 |
| 知识库 | POST | /api/kb/documents/upload | 上传文档（JSON） |
| 知识库 | POST | /api/kb/documents/upload/file | 上传文档（文件） |
| 知识库 | POST | /api/kb/search | 搜索知识库 |
| 记忆 | GET | /api/memory/history/{sessionId} | 获取对话历史 |
| 记忆 | POST | /api/memory/message | 添加消息 |
| 记忆 | POST | /api/memory/summary/{sessionId} | 生成摘要 |
| 记忆 | POST | /api/memory/long-term/store | 存储长期记忆 |
| 记忆 | POST | /api/memory/long-term/recall | 召回长期记忆 |
| 测试 | POST | /api/test/generate | 生成单元测试（JSON） |
| 测试 | POST | /api/test/generate/file | 生成单元测试（文件） |
| 测试 | POST | /api/test/coverage/analyze | 分析覆盖率（JSON） |
| 测试 | POST | /api/test/coverage/analyze/file | 分析覆盖率（文件） |
| 测试 | POST | /api/test/coverage/report | 生成覆盖率报告 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DASHSCOPE_API_KEY | 阿里云DashScope API密钥 | - |
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PORT | Redis端口 | 6379 |
| REDIS_PASSWORD | Redis密码 | - |
