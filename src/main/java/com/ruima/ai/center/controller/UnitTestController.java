package com.ruima.ai.center.controller;

import com.ruima.ai.center.model.dto.CoverageReport;
import com.ruima.ai.center.model.dto.TestGenerationRequest;
import com.ruima.ai.center.service.UnitTestService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class UnitTestController {

    @Autowired
    private UnitTestService unitTestService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateTests(@RequestBody TestGenerationRequest request) {
        String testCode = unitTestService.generateTests(request);
        return ResponseEntity.ok(Map.of("testCode", testCode));
    }

    @PostMapping("/generate/file")
    public ResponseEntity<Map<String, String>> generateFromFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
        }

        try (InputStream in = file.getInputStream()) {
            Tika tika = new Tika();
            String sourceCode = tika.parseToString(in);
            String className = file.getOriginalFilename();
            if (className != null && className.contains(".")) {
                className = className.substring(0, className.lastIndexOf('.'));
            }

            TestGenerationRequest request = new TestGenerationRequest();
            request.setTargetClassName(className);
            request.setSourceCode(sourceCode);

            String testCode = unitTestService.generateTests(request);
            return ResponseEntity.ok(Map.of("testCode", testCode));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/coverage/analyze")
    public ResponseEntity<CoverageReport> analyzeCoverage(@RequestBody Map<String, String> request) {
        String className = request.get("className");
        String sourceCode = request.get("sourceCode");
        String testCode = request.get("testCode");

        if (sourceCode == null || sourceCode.isEmpty()) {
            throw new IllegalArgumentException("源码不能为空");
        }

        CoverageReport report = unitTestService.analyzeCoverage(className, sourceCode, testCode != null ? testCode : "");
        return ResponseEntity.ok(report);
    }

    @PostMapping("/coverage/analyze/file")
    public ResponseEntity<CoverageReport> analyzeFromFiles(
            @RequestParam("sourceFile") MultipartFile sourceFile,
            @RequestParam(value = "testFile", required = false) MultipartFile testFile) {

        if (sourceFile.isEmpty()) {
            throw new IllegalArgumentException("源码文件不能为空");
        }

        try {
            Tika tika = new Tika();
            String sourceCode = tika.parseToString(sourceFile.getInputStream());
            String testCode = (testFile != null && !testFile.isEmpty())
                    ? tika.parseToString(testFile.getInputStream()) : "";

            String className = sourceFile.getOriginalFilename();
            if (className != null && className.contains(".")) {
                className = className.substring(0, className.lastIndexOf('.'));
            }

            CoverageReport report = unitTestService.analyzeCoverage(className, sourceCode, testCode);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            throw new RuntimeException("分析失败: " + e.getMessage(), e);
        }
    }

    @PostMapping("/coverage/report")
    public ResponseEntity<Map<String, String>> generateReport(@RequestBody CoverageReport report) {
        String formattedReport = unitTestService.generateCoverageReport(report);
        return ResponseEntity.ok(Map.of("report", formattedReport));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "服务异常";
        if (msg.contains("Arrearage") || msg.contains("overdue")) {
            msg = "DashScope 账户欠费，请充值后再试";
        }
        return ResponseEntity.internalServerError().body(Map.of("error", msg));
    }
}
