package com.puti.code.server.service;

public final class GraphQueryRecord {

    private GraphQueryRecord() {
    }

    public record MethodSearchRecord(
            String id,
            String name,
            String fullName,
            String type,
            String visibility,
            String branchName,
            String repoId) {
    }

    public record GraphNodeRecord(
            String id,
            String name,
            String fullName,
            String type,
            String visibility,
            Boolean isLibrary,
            Boolean sourceNode,
            Boolean daoNode,
            java.util.Map<String, Object> properties) {
    }

    public record GraphEdgeRecord(
            String source,
            String target,
            String type,
            String category,
            java.util.Map<String, Object> properties) {
    }
}
