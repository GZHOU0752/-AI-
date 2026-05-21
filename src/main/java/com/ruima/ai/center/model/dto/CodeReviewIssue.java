package com.ruima.ai.center.model.dto;

import com.ruima.ai.center.model.enums.IssueSeverity;
import com.ruima.ai.center.model.enums.ReviewDimension;

public class CodeReviewIssue {

    private IssueSeverity severity;
    private ReviewDimension dimension;
    private String filePath;
    private int lineNumber;
    private String title;
    private String description;
    private String impact;
    private String suggestion;
    private boolean resolved;
    private String resolvedBy;
    private String resolvedAt;

    public IssueSeverity getSeverity() { return severity; }
    public void setSeverity(IssueSeverity severity) { this.severity = severity; }
    public ReviewDimension getDimension() { return dimension; }
    public void setDimension(ReviewDimension dimension) { this.dimension = dimension; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
}
