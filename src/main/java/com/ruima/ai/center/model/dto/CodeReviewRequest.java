package com.ruima.ai.center.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CodeReviewRequest {

    @NotBlank(message = "PR/MR标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "变更文件列表不能为空")
    private List<FileChange> fileChanges;

    private String branchName;

    private String targetBranch;

    private List<String> focusDimensions;

    private boolean includeReview = true;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<FileChange> getFileChanges() { return fileChanges; }
    public void setFileChanges(List<FileChange> fileChanges) { this.fileChanges = fileChanges; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }
    public List<String> getFocusDimensions() { return focusDimensions; }
    public void setFocusDimensions(List<String> focusDimensions) { this.focusDimensions = focusDimensions; }
    public boolean isIncludeReview() { return includeReview; }
    public void setIncludeReview(boolean includeReview) { this.includeReview = includeReview; }

    public static class FileChange {
        private String filePath;
        private String fileName;
        private String diffContent;
        private String fullContent;
        private ChangeType changeType;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getDiffContent() { return diffContent; }
        public void setDiffContent(String diffContent) { this.diffContent = diffContent; }
        public String getFullContent() { return fullContent; }
        public void setFullContent(String fullContent) { this.fullContent = fullContent; }
        public ChangeType getChangeType() { return changeType; }
        public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
    }

    public enum ChangeType {
        ADDED, MODIFIED, DELETED, RENAMED
    }
}
