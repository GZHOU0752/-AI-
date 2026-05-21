package com.ruima.ai.center.model.dto;

import java.util.List;
import java.util.Map;

public class KnowledgeDocument {

    private String id;
    private String title;
    private String content;
    private String sourcePath;
    private String fileType;
    private List<DocumentChunk> chunks;
    private Map<String, String> metadata;
    private String uploadedBy;
    private String uploadTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public List<DocumentChunk> getChunks() { return chunks; }
    public void setChunks(List<DocumentChunk> chunks) { this.chunks = chunks; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public String getUploadTime() { return uploadTime; }
    public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }

    public static class DocumentChunk {
        private String chunkId;
        private String content;
        private int chunkIndex;
        private int startPosition;
        private int endPosition;
        private Map<String, String> metadata;

        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getStartPosition() { return startPosition; }
        public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
        public int getEndPosition() { return endPosition; }
        public void setEndPosition(int endPosition) { this.endPosition = endPosition; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }
}
