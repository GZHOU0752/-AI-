package com.ruima.ai.center.model.dto;

import java.util.List;

public class CodeReviewReport {

    private String id;
    private String title;
    private String changeIntent;
    private String impactScope;
    private double overallScore;
    private List<CodeReviewIssue> criticalIssues;
    private List<CodeReviewIssue> warningIssues;
    private List<CodeReviewIssue> infoIssues;
    private String summary;
    private String improvementDirection;
    private String reviewTime;
    private String formattedReport;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getChangeIntent() { return changeIntent; }
    public void setChangeIntent(String changeIntent) { this.changeIntent = changeIntent; }
    public String getImpactScope() { return impactScope; }
    public void setImpactScope(String impactScope) { this.impactScope = impactScope; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public List<CodeReviewIssue> getCriticalIssues() { return criticalIssues; }
    public void setCriticalIssues(List<CodeReviewIssue> criticalIssues) { this.criticalIssues = criticalIssues; }
    public List<CodeReviewIssue> getWarningIssues() { return warningIssues; }
    public void setWarningIssues(List<CodeReviewIssue> warningIssues) { this.warningIssues = warningIssues; }
    public List<CodeReviewIssue> getInfoIssues() { return infoIssues; }
    public void setInfoIssues(List<CodeReviewIssue> infoIssues) { this.infoIssues = infoIssues; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getImprovementDirection() { return improvementDirection; }
    public void setImprovementDirection(String improvementDirection) { this.improvementDirection = improvementDirection; }
    public String getReviewTime() { return reviewTime; }
    public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }
    public String getFormattedReport() { return formattedReport; }
    public void setFormattedReport(String formattedReport) { this.formattedReport = formattedReport; }
}
