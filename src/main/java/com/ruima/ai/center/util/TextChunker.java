package com.ruima.ai.center.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义分块策略 - 基于语义边界进行文本分块
 */
public class TextChunker {

    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?。！？\n])\\s+"
    );

    private static final Pattern SECTION_BOUNDARY = Pattern.compile(
            "(?:(?:^|\\n)\\s*(?:#{1,6}|第[一二三四五六七八九十]+[章节条款]|[\\d]+[.、)])\\s*)",
            Pattern.MULTILINE
    );

    private final int maxChunkSize;
    private final int chunkOverlap;

    public TextChunker() {
        this(DEFAULT_MAX_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public TextChunker(int maxChunkSize, int chunkOverlap) {
        this.maxChunkSize = maxChunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sections = splitByBoundary(text, SECTION_BOUNDARY, false);
        List<String> chunks = new ArrayList<>();

        for (String section : sections) {
            if (section.length() <= maxChunkSize) {
                if (!section.trim().isEmpty()) {
                    chunks.add(section.trim());
                }
            } else {
                chunks.addAll(splitLongSection(section));
            }
        }

        return addOverlap(chunks);
    }

    private List<String> splitByBoundary(String text, Pattern boundary, boolean fallbackToFull) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = boundary.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String part = text.substring(lastEnd, matcher.start()).trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
            }
            lastEnd = matcher.start();
        }

        if (lastEnd < text.length()) {
            String lastPart = text.substring(lastEnd).trim();
            if (!lastPart.isEmpty()) {
                parts.add(lastPart);
            }
        }

        return parts.isEmpty() && fallbackToFull
                ? java.util.Collections.singletonList(text)
                : parts;
    }

    private List<String> splitLongSection(String section) {
        List<String> sentences = splitByBoundary(section, SENTENCE_BOUNDARY, true);
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> addOverlap(List<String> chunks) {
        if (chunkOverlap <= 0 || chunks.size() <= 1) {
            return chunks;
        }

        List<String> overlappedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (i > 0) {
                String prevChunk = chunks.get(i - 1);
                int overlapStart = Math.max(0, prevChunk.length() - chunkOverlap);
                String overlap = prevChunk.substring(overlapStart);
                chunk = overlap + "\n" + chunk;
            }
            overlappedChunks.add(chunk);
        }

        return overlappedChunks;
    }
}
