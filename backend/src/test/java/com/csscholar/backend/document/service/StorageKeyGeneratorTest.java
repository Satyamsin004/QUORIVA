package com.csscholar.backend.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StorageKeyGeneratorTest {

    private StorageKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StorageKeyGenerator();
    }

    @Test
    @DisplayName("Should generate safe storage key preserving workspace and document IDs")
    void shouldGenerateSafeStorageKey() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String originalFilename = "Lecture 01 - Intro to OS.pdf";

        String key = generator.generateStorageKey(workspaceId, documentId, originalFilename);

        assertThat(key).startsWith("workspaces/" + workspaceId + "/documents/" + documentId + "/");
        assertThat(key).endsWith(".pdf");
        assertThat(key).doesNotContain(" ");
    }

    @Test
    @DisplayName("Should sanitize special characters in original filename")
    void shouldSanitizeSpecialCharacters() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String maliciousFilename = "my*document#name@with!symbols.docx";

        String key = generator.generateStorageKey(workspaceId, documentId, maliciousFilename);

        assertThat(key).doesNotContain("*").doesNotContain("#").doesNotContain("@").doesNotContain("!");
        assertThat(key).endsWith(".docx");
    }

    @Test
    @DisplayName("Should truncate excessively long filename base while preserving extension")
    void shouldTruncateExcessivelyLongFilename() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String longFilename = "a".repeat(150) + ".pptx";

        String key = generator.generateStorageKey(workspaceId, documentId, longFilename);

        assertThat(key).endsWith(".pptx");
        String filenamePart = key.substring(key.lastIndexOf('/') + 1);
        assertThat(filenamePart.length()).isLessThanOrEqualTo(60);
    }
}

