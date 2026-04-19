package com.puti.code.app.dependency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyManifestTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyMapWhenFileNotExists() {
        Path manifest = tempDir.resolve("not-exist.json");
        Map<String, String> result = DependencyManifest.load(manifest);
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndLoadRoundTrip() {
        Path manifest = tempDir.resolve(".library").resolve("dependency-manifest.json");

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("org.springframework.boot:spring-boot:2.7.0", "runtime");
        deps.put("org.mybatis:mybatis:3.5.13", "runtime");

        DependencyManifest.save(manifest, deps);

        assertTrue(DependencyManifest.exists(manifest));

        Map<String, String> loaded = DependencyManifest.load(manifest);
        assertEquals(2, loaded.size());
        assertEquals("runtime", loaded.get("org.springframework.boot:spring-boot:2.7.0"));
        assertEquals("runtime", loaded.get("org.mybatis:mybatis:3.5.13"));
    }

    @Test
    void loadReturnsEmptyMapOnCorruptFile() {
        Path manifest = tempDir.resolve("corrupt.json");
        PathAssertions.writeText(manifest, "not valid json {{{");

        Map<String, String> result = DependencyManifest.load(manifest);
        assertTrue(result.isEmpty());
    }

    @Test
    void defaultManifestPathContainsProjectRoot() {
        Path path = DependencyManifest.defaultManifestPath("/home/user/project");
        assertEquals("/home/user/project/.library/dependency-manifest.json", path.toString());
    }

    @Test
    void existsReturnsFalseForNonExistentFile() {
        assertFalse(DependencyManifest.exists(tempDir.resolve("no-file.json")));
    }

    @Test
    void saveCreatesParentDirectories() {
        Path manifest = tempDir.resolve("a").resolve("b").resolve("c").resolve("manifest.json");
        DependencyManifest.save(manifest, Map.of("com.example:lib:1.0", "runtime"));
        assertTrue(DependencyManifest.exists(manifest));
    }

    /** Helper to avoid depending on Files.writeString (Java 11+) in test code. */
    private static class PathAssertions {
        static void writeText(Path path, String content) {
            try {
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.writeString(path, content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
