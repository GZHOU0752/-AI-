- ## 技术栈
框架: Spring Boot 2.7.18 + Java 17
LLM框架: Langchain4j 0.30.0
大模型: Qwen-plus (通过DashScope API)
向量模型: text-embedding-v3
向量数据库: InMemoryEmbeddingStore（内置）/可切换PineCone
文档解析: Apache Tika
记忆存储**: Redis (Redisson)
前端: Vue 3 + Vite + Element Plus
测试: JUnit 5 + Mockito 4.5.1

- ## 环境变量
| 变量 | 说明 | 默认值 |
| DASHSCOPE_API_KEY | 阿里云DashScope API密钥 | - |
| REDIS_HOST | Redis主机 |-|
| REDIS_PORT | Redis端口 |-|
| REDIS_PASSWORD | Redis密码 | - |
