package com.ruima.ai.center.controller;

import com.google.gson.Gson;
import com.ruima.ai.center.model.dto.CodeReviewReport;
import com.ruima.ai.center.model.dto.CodeReviewRequest;
import com.ruima.ai.center.service.AiCodeReviewService;
import org.apache.tika.Tika;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/aicr")
public class AiCodeReviewController {

    @Autowired
    private AiCodeReviewService aiCodeReviewService;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    private static final Gson gson = new Gson();

    @PostMapping("/review")
    public ResponseEntity<CodeReviewReport> review(@Valid @RequestBody CodeReviewRequest request,
                                                    @RequestParam(value = "userId", required = false) String userId) {
        CodeReviewReport report = aiCodeReviewService.review(request);
        saveToHistory(userId, report);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/review/files")
    public ResponseEntity<CodeReviewReport> reviewFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "dimensions", required = false) String dimensions,
            @RequestParam(value = "userId", required = false) String userId) {

        if (files.isEmpty()) throw new IllegalArgumentException("至少上传一个文件");

        Tika tika = new Tika();
        CodeReviewRequest request = new CodeReviewRequest();
        request.setTitle("文件评审: " + files.get(0).getOriginalFilename());
        request.setFileChanges(new ArrayList<>());

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try (InputStream in = file.getInputStream()) {
                CodeReviewRequest.FileChange fc = new CodeReviewRequest.FileChange();
                fc.setFilePath(file.getOriginalFilename());
                fc.setFileName(file.getOriginalFilename());
                fc.setDiffContent(tika.parseToString(in));
                fc.setChangeType(CodeReviewRequest.ChangeType.MODIFIED);
                request.getFileChanges().add(fc);
            } catch (Exception e) {
                CodeReviewRequest.FileChange fc = new CodeReviewRequest.FileChange();
                fc.setFilePath(file.getOriginalFilename());
                fc.setFileName(file.getOriginalFilename());
                fc.setDiffContent("[无法解析: " + e.getMessage() + "]");
                fc.setChangeType(CodeReviewRequest.ChangeType.MODIFIED);
                request.getFileChanges().add(fc);
            }
        }

        if (dimensions != null && !dimensions.isEmpty()) {
            request.setFocusDimensions(Arrays.asList(dimensions.split(",")));
        }

        CodeReviewReport report = aiCodeReviewService.review(request);
        saveToHistory(userId, report);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> listHistory(@RequestParam("userId") String userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (redissonClient == null) return ResponseEntity.ok(result);

        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet("ruima:aicr:history:" + userId);
        for (String json : set.valueRangeReversed(0, 19)) {
            try {
                Map<String, Object> item = new HashMap<>();
                CodeReviewReport r = gson.fromJson(json, CodeReviewReport.class);
                if (r != null && r.getTitle() != null) {
                    item.put("id", r.getId());
                    item.put("title", r.getTitle());
                    item.put("score", r.getOverallScore());
                    item.put("critical", r.getCriticalIssues() != null ? r.getCriticalIssues().size() : 0);
                    item.put("warning", r.getWarningIssues() != null ? r.getWarningIssues().size() : 0);
                    item.put("info", r.getInfoIssues() != null ? r.getInfoIssues().size() : 0);
                    item.put("time", r.getReviewTime());
                    item.put("data", gson.toJson(r));
                    result.add(item);
                }
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Map<String, String>> deleteHistory(
            @PathVariable String id,
            @RequestParam("userId") String userId) {
        if (redissonClient != null) {
            RScoredSortedSet<String> set = redissonClient.getScoredSortedSet("ruima:aicr:history:" + userId);
            set.removeAll(set.valueRange(0, -1).stream().filter(j -> j.contains(id)).toList());
        }
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    private void saveToHistory(String userId, CodeReviewReport report) {
        if (redissonClient == null || userId == null) return;
        try {
            RScoredSortedSet<String> set = redissonClient.getScoredSortedSet("ruima:aicr:history:" + userId);
            set.add(System.currentTimeMillis(), gson.toJson(report));
            // 最多保留 50 条
            if (set.size() > 50) set.removeRangeByRank(0, set.size() - 51);
        } catch (Exception ignored) {}
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "AI Code Review",
                "status", "running",
                "dimensions", 8,
                "severityLevels", new String[]{"Critical", "Warning", "Info"}
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "服务异常";
        if (msg.contains("Arrearage") || msg.contains("overdue")) msg = "DashScope 账户欠费，请充值后再试";
        return ResponseEntity.internalServerError().body(Map.of("error", msg));
    }
}
