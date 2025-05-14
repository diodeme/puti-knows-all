package com.puti.code.repository.graph;

import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.GraphStorageType;
import com.puti.code.repository.graph.file.LocalFileGraphStorageRepository;
import com.puti.code.repository.graph.nebula.NebulaGraphStorageRepository;

/**
 * 图存储仓储工厂。
 */
public final class GraphStorageRepositoryFactory {

    private GraphStorageRepositoryFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static GraphStorageRepository create(AppConfig config) {
        GraphStorageType storageType = GraphStorageType.fromValue(config.getGraphStorageType());
        return switch (storageType) {
            case LOCAL_FILE -> new LocalFileGraphStorageRepository();
            case NEO4J -> throw new UnsupportedOperationException("Graph storage backend neo4j is not implemented yet");
            case NEBULA -> new NebulaGraphStorageRepository();
        };
    }
}
