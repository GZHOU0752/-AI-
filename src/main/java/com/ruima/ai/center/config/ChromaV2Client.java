package com.ruima.ai.center.config;

import com.google.gson.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chroma v2 API 适配器，实现 langchain4j EmbeddingStore 接口。
 */
public class ChromaV2Client implements EmbeddingStore<TextSegment> {

    private static final String API_BASE = "/api/v2/tenants/default_tenant/databases/default_database";

    private final HttpClient http;
    private final String baseUrl;
    private final String collectionName;
    private String collectionId;

    public ChromaV2Client(String baseUrl, String collectionName) {
        this.baseUrl = baseUrl;
        this.collectionName = collectionName;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.collectionId = getOrCreateCollection();
    }

    private String getOrCreateCollection() {
        try {
            // 先查已有 collection
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_BASE + "/collections"))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
            for (var el : arr) {
                JsonObject c = el.getAsJsonObject();
                if (collectionName.equals(c.get("name").getAsString())) {
                    return c.get("id").getAsString();
                }
            }
            // 不存在则创建
            JsonObject body = new JsonObject();
            body.addProperty("name", collectionName);
            HttpRequest post = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_BASE + "/collections"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> r2 = http.send(post, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(r2.body()).getAsJsonObject().get("id").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Chroma 初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String add(Embedding embedding) {
        return add(embedding, null);
    }

    @Override
    public void add(String id, Embedding embedding) {
        doAdd(Collections.singletonList(id), Collections.singletonList(embedding), null);
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString();
        doAdd(Collections.singletonList(id), Collections.singletonList(embedding),
                segment != null ? Collections.singletonList(segment) : null);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return addAll(embeddings, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = embeddings.stream().map(e -> UUID.randomUUID().toString()).collect(Collectors.toList());
        doAdd(ids, embeddings, segments);
        return ids;
    }

    private void doAdd(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
        try {
            JsonObject body = new JsonObject();
            JsonArray idsArr = new JsonArray(); ids.forEach(idsArr::add);
            body.add("ids", idsArr);

            JsonArray embArr = new JsonArray();
            for (Embedding e : embeddings) {
                JsonArray vec = new JsonArray();
                for (float v : e.vector()) vec.add(v);
                embArr.add(vec);
            }
            body.add("embeddings", embArr);

            if (segments != null) {
                JsonArray docs = new JsonArray();
                JsonArray metas = new JsonArray();
                for (TextSegment s : segments) {
                    docs.add(s.text());
                    JsonObject meta = new JsonObject();
                    s.metadata().toMap().forEach((k, v) -> meta.addProperty(k, String.valueOf(v)));
                    metas.add(meta);
                }
                body.add("documents", docs);
                body.add("metadatas", metas);
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_BASE + "/collections/" + collectionId + "/add"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Chroma add 失败: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding queryEmbedding, int maxResults) {
        return findRelevant(queryEmbedding, maxResults, 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding queryEmbedding, int maxResults, double minScore) {
        try {
            JsonObject body = new JsonObject();
            JsonArray embArr = new JsonArray();
            for (float v : queryEmbedding.vector()) embArr.add(v);
            body.add("query_embeddings", JsonParser.parseString(embArr.toString()));
            body.addProperty("n_results", maxResults);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_BASE + "/collections/" + collectionId + "/query"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject result = JsonParser.parseString(resp.body()).getAsJsonObject();

            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            JsonArray idsArr = result.getAsJsonArray("ids");
            JsonArray distArr = result.getAsJsonArray("distances");
            JsonArray docsArr = result.getAsJsonArray("documents");

            if (idsArr == null || idsArr.isEmpty()) return matches;

            JsonArray batch0 = idsArr.get(0).getAsJsonArray();
            JsonArray dist0 = distArr != null ? distArr.get(0).getAsJsonArray() : null;
            JsonArray docs0 = docsArr != null ? docsArr.get(0).getAsJsonArray() : null;

            for (int i = 0; i < batch0.size(); i++) {
                double distance = dist0 != null ? dist0.get(i).getAsDouble() : 0;
                double score = 1.0 - distance / 2.0; // Chroma 返回 L2 距离，转为相似度
                if (score < minScore) continue;

                String docText = docs0 != null && !docs0.get(i).isJsonNull() ? docs0.get(i).getAsString() : null;
                TextSegment seg = docText != null ? TextSegment.from(docText) : null;
                matches.add(new EmbeddingMatch<>(score, batch0.get(i).getAsString(), null, seg));
            }
            return matches;
        } catch (Exception e) {
            throw new RuntimeException("Chroma query 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按文档 ID 删除所有相关向量
     */
    public void deleteByDocumentId(String documentId) {
        try {
            JsonObject where = new JsonObject();
            where.addProperty("document_id", documentId);
            JsonObject body = new JsonObject();
            body.add("where", where);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_BASE + "/collections/" + collectionId + "/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Chroma delete 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 collection 中的向量总数
     */
    public int count() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_BASE + "/collections/" + collectionId))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int count = JsonParser.parseString(resp.body()).getAsJsonObject()
                    .get("metadata").getAsJsonObject()
                    .get("size").getAsInt();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
