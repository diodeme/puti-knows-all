package com.puti.code.analyzer.java.context;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.base.enums.ParseType;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import com.puti.code.repository.graph.GraphStorageRepository;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GraphContext {
    private GraphStorageRepository graphStorageRepository;
    private GraphVectorMilvusClient graphVectorMilvusClient;
    private VectorGenerator vectorGenerator;
    private ParseType parseType;
}
