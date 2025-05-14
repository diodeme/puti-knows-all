package com.puti.code.repository.graph.file;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.model.FileNode;
import com.puti.code.base.model.NodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileGraphStorageRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteNodesAndEdgesAsJsonLines() throws IOException {
        LocalFileGraphStorageRepository repository = new LocalFileGraphStorageRepository(tempDir);
        try {
            FileNode node = FileNode.builder()
                    .id("file-1")
                    .nodeType(NodeType.FILE)
                    .fullName("src/main/java/Demo.java")
                    .filePath("src/main/java/Demo.java")
                    .name("Demo.java")
                    .extension("java")
                    .build();
            Edge edge = Edge.builder()
                    .srcId("file-1")
                    .dstId("class-1")
                    .type(EdgeType.CONTAINS)
                    .lineNumber(12)
                    .build();

            repository.insertNode(node);
            repository.insertEdge(edge);
        } finally {
            repository.close();
        }

        List<String> nodeLines = Files.readAllLines(tempDir.resolve("nodes.jsonl"));
        List<String> edgeLines = Files.readAllLines(tempDir.resolve("edges.jsonl"));

        assertEquals(1, nodeLines.size());
        assertEquals(1, edgeLines.size());
        assertTrue(nodeLines.get(0).contains("\"id\":\"file-1\""));
        assertTrue(nodeLines.get(0).contains("\"tag\":\"file\""));
        assertTrue(edgeLines.get(0).contains("\"type\":\"contains\""));
        assertTrue(edgeLines.get(0).contains("\"line_number\":12"));
    }
}
