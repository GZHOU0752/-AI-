package com.ruima.ai.center.controller;

import com.ruima.ai.center.model.dto.CodeReviewReport;
import com.ruima.ai.center.model.dto.CodeReviewRequest;
import com.ruima.ai.center.service.AiCodeReviewService;
import org.apache.tika.Tika;
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

    @PostMapping("/review")
    public ResponseEntity<CodeReviewReport> review(@Valid @RequestBody CodeReviewRequest request) {
        CodeReviewReport report = aiCodeReviewService.review(request);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/review/files")
    public ResponseEntity<CodeReviewReport> reviewFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "dimensions", required = false) String dimensions) {

        if (files.isEmpty()) {
            throw new IllegalArgumentException("至少上传一个文件");
        }

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
        return ResponseEntity.ok(report);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "服务异常";
        if (msg.contains("Arrearage") || msg.contains("overdue")) {
            msg = "DashScope 账户欠费，请充值后再试";
        }
        return ResponseEntity.internalServerError().body(Map.of("error", msg));
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
}
