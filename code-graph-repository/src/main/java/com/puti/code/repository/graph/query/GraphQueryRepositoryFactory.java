package com.puti.code.repository.graph.query;

import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.GraphStorageType;
import com.puti.code.repository.graph.file.LocalFileGraphQueryRepository;
import com.puti.code.repository.graph.nebula.NebulaGraphQueryRepository;

/**
 * 图查询仓储工厂。
 */
public final class GraphQueryRepositoryFactory {

    private GraphQueryRepositoryFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static GraphQueryRepository create(AppConfig config) {
        GraphStorageType storageType = GraphStorageType.fromValue(config.getGraphStorageType());
        return switch (storageType) {
            case LOCAL_FILE -> new LocalFileGraphQueryRepository();
            case NEO4J -> throw new UnsupportedOperationException("Graph query backend neo4j is not implemented yet");
            case NEBULA -> new NebulaGraphQueryRepository();
        };
    }
}
