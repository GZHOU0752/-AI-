package com.ruima.ai.center.model.dto;

import java.util.List;

public class CoverageReport {

    private String className;
    private double lineCoverage;
    private double branchCoverage;
    private double methodCoverage;
    private double exceptionCoverage;
    private String overallRating;
    private List<CoverageScenario> coveredScenarios;
    private List<UncoveredScenario> uncoveredScenarios;
    private List<BranchAnalysis> branchAnalyses;
    private List<MethodCoverage> methodCoverages;
    private List<TestCaseSuggestion> testSuggestions;
    private String riskLevel;
    private String suggestedFixTime;

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public double getLineCoverage() { return lineCoverage; }
    public void setLineCoverage(double lineCoverage) { this.lineCoverage = lineCoverage; }
    public double getBranchCoverage() { return branchCoverage; }
    public void setBranchCoverage(double branchCoverage) { this.branchCoverage = branchCoverage; }
    public double getMethodCoverage() { return methodCoverage; }
    public void setMethodCoverage(double methodCoverage) { this.methodCoverage = methodCoverage; }
    public double getExceptionCoverage() { return exceptionCoverage; }
    public void setExceptionCoverage(double exceptionCoverage) { this.exceptionCoverage = exceptionCoverage; }
    public String getOverallRating() { return overallRating; }
    public void setOverallRating(String overallRating) { this.overallRating = overallRating; }
    public List<CoverageScenario> getCoveredScenarios() { return coveredScenarios; }
    public void setCoveredScenarios(List<CoverageScenario> coveredScenarios) { this.coveredScenarios = coveredScenarios; }
    public List<UncoveredScenario> getUncoveredScenarios() { return uncoveredScenarios; }
    public void setUncoveredScenarios(List<UncoveredScenario> uncoveredScenarios) { this.uncoveredScenarios = uncoveredScenarios; }
    public List<BranchAnalysis> getBranchAnalyses() { return branchAnalyses; }
    public void setBranchAnalyses(List<BranchAnalysis> branchAnalyses) { this.branchAnalyses = branchAnalyses; }
    public List<MethodCoverage> getMethodCoverages() { return methodCoverages; }
    public void setMethodCoverages(List<MethodCoverage> methodCoverages) { this.methodCoverages = methodCoverages; }
    public List<TestCaseSuggestion> getTestSuggestions() { return testSuggestions; }
    public void setTestSuggestions(List<TestCaseSuggestion> testSuggestions) { this.testSuggestions = testSuggestions; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSuggestedFixTime() { return suggestedFixTime; }
    public void setSuggestedFixTime(String suggestedFixTime) { this.suggestedFixTime = suggestedFixTime; }

    public static class CoverageScenario {
        private String category;
        private String description;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UncoveredScenario {
        private String priority;
        private String description;
        private String risk;
        private String impact;
        private String suggestion;

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }
        public String getImpact() { return impact; }
        public void setImpact(String impact) { this.impact = impact; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }

    public static class BranchAnalysis {
        private int lineNumber;
        private String condition;
        private String missingTest;
        private String priority;

        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getMissingTest() { return missingTest; }
        public void setMissingTest(String missingTest) { this.missingTest = missingTest; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    public static class MethodCoverage {
        private String methodName;
        private String visibility;
        private boolean covered;
        private String priority;
        private String description;

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
        public boolean isCovered() { return covered; }
        public void setCovered(boolean covered) { this.covered = covered; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TestCaseSuggestion {
        private String scenario;
        private String methodName;
        private String displayName;
        private String testCode;

        public String getScenario() { return scenario; }
        public void setScenario(String scenario) { this.scenario = scenario; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getTestCode() { return testCode; }
        public void setTestCode(String testCode) { this.testCode = testCode; }
    }
}
