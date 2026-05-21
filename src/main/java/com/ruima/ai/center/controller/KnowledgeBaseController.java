package com.ruima.ai.center.controller;

import com.ruima.ai.center.model.dto.ChatMessage;
import com.ruima.ai.center.model.dto.KnowledgeDocument;
import com.ruima.ai.center.service.KnowledgeBaseService;
import com.ruima.ai.center.service.MemoryService;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private MemoryService memoryService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "default");
        String userId = request.getOrDefault("userId", "anonymous");
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "问题不能为空"));
        }

        String answer = knowledgeBaseService.ask(sessionId, userId, question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @GetMapping("/context/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getContext(@PathVariable String sessionId) {
        List<ChatMessage> context = knowledgeBaseService.getContext(sessionId);
        return ResponseEntity.ok(context);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, String>>> listSessions(
            @RequestParam(value = "userId", required = false) String userId) {
        return ResponseEntity.ok(memoryService.listSessions(userId));
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<Map<String, String>> uploadDocument(@RequestBody KnowledgeDocument document) {
        knowledgeBaseService.uploadDocument(document);
        return ResponseEntity.ok(Map.of("status", "success", "message", "文档已上传并入库"));
    }

    @PostMapping("/documents/upload/file")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
        }

        try (InputStream in = file.getInputStream()) {
            Tika tika = new Tika();
            String content = tika.parseToString(in);

            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setTitle(file.getOriginalFilename());
            doc.setContent(content);
            doc.setFileType(tika.detect(file.getOriginalFilename()));
            doc.setSourcePath(file.getOriginalFilename());

            knowledgeBaseService.uploadDocument(doc);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "文件已解析并入库",
                    "fileType", doc.getFileType(),
                    "contentLength", content.length()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "文件解析失败: " + e.getMessage()));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        List<Map<String, Object>> results = knowledgeBaseService.searchKnowledge(query);
        return ResponseEntity.ok(results);
    }
}
