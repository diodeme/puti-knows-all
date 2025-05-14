package com.puti.code.analyzer.java.processor;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.analyzer.java.strategy.NodeContentStrategyFactory;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.Node;
import com.puti.code.base.model.NodeType;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import org.slf4j.Logger;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtElement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象处理器
 *
 * @param <T> 元素类型
 */
public abstract class BaseProcessor<T extends CtElement> extends AbstractProcessor<T> {
    protected final AppConfig config = AppConfig.getInstance();
    protected final GraphStorageRepository graphStorageRepository;
    protected final GraphVectorMilvusClient graphVectorMilvusClient;
    protected final VectorGenerator vectorGenerator;
    protected final ParseType parseType;

    // 节点缓存，用于批量处理
    private final List<Node> nodeBuffer = new ArrayList<>();
    // 边缓存，用于批量处理
    private final List<Edge> edgeBuffer = new ArrayList<>();
    // 缓冲区大小
    private static final int BUFFER_SIZE = 100;
    // 处理计数器
    protected final AtomicInteger processedCount = new AtomicInteger(0);
    // 边处理计数器
    protected final AtomicInteger processedEdgeCount = new AtomicInteger(0);

    public BaseProcessor(GraphContext graphContext) {
        this.graphStorageRepository = graphContext.getGraphStorageRepository();
        this.graphVectorMilvusClient = graphContext.getGraphVectorMilvusClient();
        this.vectorGenerator = graphContext.getVectorGenerator();
        this.parseType = graphContext.getParseType();
    }

    /**
     * 处理节点
     *
     * @param node 节点
     */
    protected void processNode(Node node) {
        try {
            if(NodeType.MARKER_ANNOTATION.equals(node.getNodeType())){
                return;
            }
            getLogger().info("Processing node: {} fullName:{}", node.getId(), node.getFullName());
            // 生成向量
            String content = getNodeContent(node);

//            if(ParseType.PROJECT.equals(parseType)) {
//                float[] vector = vectorGenerator.generateVector(content);
//                node.setVector(vector);
//            }

            // 添加到缓冲区
            synchronized (nodeBuffer) {
                nodeBuffer.add(node);

                // 当缓冲区达到指定大小时，批量处理
                if (nodeBuffer.size() >= BUFFER_SIZE) {
                    flushNodes();
                }
            }

            // 增加处理计数
            processedCount.incrementAndGet();
        } catch (Exception e) {
            getLogger().error("Failed to process node: {}", node.getId(), e);
        }
    }

    /**
     * 获取节点内容
     * 使用策略模式根据节点类型获取对应的内容
     *
     * @param node 节点
     * @return 节点内容
     */
    protected String getNodeContent(Node node) {
        try {
            String vectorOriContent = NodeContentStrategyFactory.getStrategy(node).getDigest(node, getFactory());
            node.setVectorOriContent(vectorOriContent);
            return vectorOriContent;
        } catch (Exception e) {
            getLogger().error("Failed to get node content for node: {}", node.getId(), e);
            String errorContent = "Error getting content for " + node.getNodeType().getValue() + ": " + node.getFullName();
            node.setVectorOriContent(errorContent);
            return errorContent;
        }
    }

    // getFactory()方法已经从AbstractProcessor继承，无需重新定义

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 将缓冲区中的节点批量处理
     */
    protected void flushNodes() {
        synchronized (nodeBuffer) {
            if (!nodeBuffer.isEmpty()) {
                try {
                    // 批量插入到图存储
                    graphStorageRepository.batchInsertNodes(new ArrayList<>(nodeBuffer));
//                    if(ParseType.PROJECT.equals(parseType)) {
//                        // 批量插入到Milvus
//                        graphVectorMilvusClient.batchUpsertNodeVectors(new ArrayList<>(nodeBuffer));
//                    }

                    getLogger().info("Batch processed {} nodes", nodeBuffer.size());
                    nodeBuffer.clear();
                } catch (Exception e) {
                    getLogger().error("Failed to batch process nodes", e);
                }
            }
        }
    }

    /**
     * 将缓冲区中的边批量处理
     */
    protected void flushEdges() {
        synchronized (edgeBuffer) {
            if (!edgeBuffer.isEmpty()) {
                try {
                    // 批量插入到图存储
                    graphStorageRepository.batchInsertEdges(new ArrayList<>(edgeBuffer));

                    getLogger().info("Batch processed {} edges", edgeBuffer.size());
                    edgeBuffer.clear();
                } catch (Exception e) {
                    getLogger().error("Failed to batch process edges", e);
                }
            }
        }
    }

    /**
     * 处理完成时调用，确保所有缓冲的节点和边都被处理
     */
    @Override
    public void processingDone() {
        flushNodes();
        flushEdges();
        getLogger().info("Processing completed. Total processed nodes: {}, edges: {}",
                processedCount.get(), processedEdgeCount.get());
        super.processingDone();
    }

    /**
     * 处理边
     *
     * @param edge 边
     */
    protected void processEdge(Edge edge) {
        try {
            getLogger().info("Processing edge {} to {}", edge.getSrcId(), edge.getDstId());
            // 添加到缓冲区
            synchronized (edgeBuffer) {
                edgeBuffer.add(edge);

                // 当缓冲区达到指定大小时，批量处理
                if (edgeBuffer.size() >= BUFFER_SIZE) {
                    flushEdges();
                }
            }

            // 增加处理计数
            processedEdgeCount.incrementAndGet();
        } catch (Exception e) {
            getLogger().error("Failed to process edge: {} -> {}", edge.getSrcId(), edge.getDstId(), e);
        }
    }

    abstract Logger getLogger();
}
