package com.ruima.ai.center.controller;

import com.ruima.ai.center.model.dto.ChatMessage;
import com.ruima.ai.center.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = memoryService.getConversationHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> addMessage(@RequestBody ChatMessage message) {
        memoryService.addMessage(message.getSessionId(), message);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/summary/{sessionId}")
    public ResponseEntity<Map<String, String>> generateSummary(@PathVariable String sessionId) {
        String summary = memoryService.generateSummary(sessionId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @PostMapping("/long-term/store")
    public ResponseEntity<Map<String, String>> storeLongTerm(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String content = request.get("content");
        String category = request.getOrDefault("category", "general");
        memoryService.storeLongTerm(userId, content, category);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/long-term/recall")
    public ResponseEntity<Map<String, Object>> recallLongTerm(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String query = request.get("query");
        List<String> memories = memoryService.recallLongTerm(userId, query);
        return ResponseEntity.ok(Map.of("memories", memories));
    }
}
