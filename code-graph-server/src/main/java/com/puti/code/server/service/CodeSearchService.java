package com.puti.code.server.service;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.util.ContentCompressor;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import com.puti.code.repository.milvus.MilvusSearchClient;
import com.puti.code.server.dao.GraphQueryDao;
import com.puti.code.server.dto.CodeSearchData;
import com.puti.code.server.dto.CodeSearchRequest;
import com.puti.code.server.dto.EntryPointsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeSearchService {

    private final MilvusSearchClient milvusSearchClient;
    private final VectorGenerator vectorGenerator;
    private final GraphQueryDao graphQueryDao;

    private static final Map<String, Float> DEFAULT_TYPE_WEIGHTS = Map.ofEntries(
            Map.entry("function", 1.0f),
            Map.entry("class", 0.8f),
            Map.entry("field", 0.6f),
            Map.entry("comment", 0.4f),
            Map.entry("annotations", 0.2f),
            Map.entry("marker_annotations", 0.2f),
            Map.entry("file", 0.1f)
    );

    public CodeSearchData search(CodeSearchRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        String strategy = request.getStrategy() != null ? request.getStrategy() : "weighted";

        // 1. 生成查询向量
        float[] queryVector = vectorGenerator.generateVector(request.getQuery());

        // 2. 向量搜索，取 topK * 10 候选集（大候选集保证类型多样性，避免单一类型占满）
        int candidateSize = Math.max(topK * 10, 100);
        List<MilvusSearchClient.MilvusSearchResult> candidates = milvusSearchClient.search(queryVector, candidateSize);

        // 3. 应用策略筛选 topK 条
        List<MilvusSearchClient.MilvusSearchResult> filtered = applyStrategy(candidates, topK, strategy,
                request.getTypeWeights());

        // 4. 批量查询 NebulaGraph 获取完整节点信息 + 可选上下文
        List<CodeSearchData.SearchResult> results = new ArrayList<>();
        int contextNodesCount = 0;

        for (MilvusSearchClient.MilvusSearchResult candidate : filtered) {
            CodeSearchData.SearchResult searchResult = buildSearchResult(candidate);

            // 填充节点详情（content, is_library, source_code_location 等）
            Optional<GraphQueryNode> nodeOpt = graphQueryDao.getNodeDetail(candidate.getId());
            if (nodeOpt.isPresent()) {
                enrichSearchResult(searchResult, nodeOpt.get());
            }

            // 填充上下文（上下游调用链）
            if (Boolean.TRUE.equals(request.getIncludeContext())) {
                int depth = request.getContextDepth() != null ? request.getContextDepth() : 1;
                List<String> contextEdgeTypes = GraphQueryService.resolveEdgeTypes(request.getEdgeTypes());
                CodeSearchData.SearchContext context = buildContext(candidate.getId(), depth, contextEdgeTypes);
                searchResult.setContext(context);
                if (context != null) {
                    contextNodesCount += countContextNodes(context);
                }
            }

            results.add(searchResult);
        }

        // 5. 组装响应
        CodeSearchData data = new CodeSearchData();
        data.setQuery(request.getQuery());
        data.setResults(results);

        CodeSearchData.SearchMeta meta = new CodeSearchData.SearchMeta();
        meta.setTotalVectorHits(candidates.size());
        meta.setReturned(results.size());
        meta.setTypeDistribution(computeTypeDistribution(results));
        meta.setStrategyUsed(strategy);
        meta.setContextNodesCount(contextNodesCount);
        data.setMeta(meta);

        return data;
    }

    public EntryPointsData getEntryPoints(String repoId, String branchName) {
        // 参数为空时使用 AppConfig 默认值
        if (repoId == null || repoId.isBlank()) {
            repoId = AppConfig.getInstance().getProjectId();
        }
        if (branchName == null || branchName.isBlank()) {
            branchName = AppConfig.getInstance().getBranch();
        }

        List<GraphQueryNode> entryPointNodes = graphQueryDao.getEntryPoints(repoId, branchName);

        List<EntryPointsData.EntryPoint> entryPoints = new ArrayList<>();
        for (GraphQueryNode node : entryPointNodes) {
            Map<String, Object> props = node.getProperties();
            EntryPointsData.EntryPoint ep = new EntryPointsData.EntryPoint();
            ep.setId(node.getId());
            ep.setNodeType(getStringProp(props, "node_type", node.getTag()));
            ep.setFullName(getStringProp(props, "full_name", ""));
            ep.setName(getStringProp(props, "name", ""));
            ep.setSourceCodeLocation(buildSourceCodeLocationWithFallback(props, node.getId()));
            entryPoints.add(ep);
        }

        EntryPointsData data = new EntryPointsData();
        data.setEntryPoints(entryPoints);

        EntryPointsData.EntryPointsMeta meta = new EntryPointsData.EntryPointsMeta();
        meta.setTotal(entryPoints.size());
        meta.setReturned(entryPoints.size());
        data.setMeta(meta);

        return data;
    }

    // ---- 策略实现 ----

    private List<MilvusSearchClient.MilvusSearchResult> applyStrategy(
            List<MilvusSearchClient.MilvusSearchResult> candidates,
            int topK, String strategy, Map<String, Float> customWeights) {

        return switch (strategy) {
            case "flat" -> candidates.stream().limit(topK).toList();
            case "per_type" -> applyPerTypeStrategy(candidates, topK);
            case "auto" -> applyAutoStrategy(candidates, topK);
            default -> applyWeightedStrategy(candidates, topK,
                    customWeights != null ? customWeights : DEFAULT_TYPE_WEIGHTS);
        };
    }

    private List<MilvusSearchClient.MilvusSearchResult> applyWeightedStrategy(
            List<MilvusSearchClient.MilvusSearchResult> candidates, int topK,
            Map<String, Float> weights) {
        return candidates.stream()
                .sorted((a, b) -> {
                    float scoreA = a.getScore() * weights.getOrDefault(a.getNodeType(), 0.1f);
                    float scoreB = b.getScore() * weights.getOrDefault(b.getNodeType(), 0.1f);
                    return Float.compare(scoreB, scoreA);
                })
                .limit(topK)
                .toList();
    }

    private List<MilvusSearchClient.MilvusSearchResult> applyAutoStrategy(
            List<MilvusSearchClient.MilvusSearchResult> candidates, int topK) {
        // 按类型统计数量
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (var c : candidates) {
            typeCounts.merge(c.getNodeType(), 1L, Long::sum);
        }
        long totalNodes = candidates.size();

        // IDF 权重: log(N / count(t))
        Map<String, Float> autoWeights = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            autoWeights.put(entry.getKey(), (float) Math.log((double) totalNodes / entry.getValue()));
        }

        return applyWeightedStrategy(candidates, topK, autoWeights);
    }

    private List<MilvusSearchClient.MilvusSearchResult> applyPerTypeStrategy(
            List<MilvusSearchClient.MilvusSearchResult> candidates, int topK) {
        // 按类型分组
        Map<String, List<MilvusSearchClient.MilvusSearchResult>> groups = new LinkedHashMap<>();
        for (var c : candidates) {
            groups.computeIfAbsent(c.getNodeType(), k -> new ArrayList<>()).add(c);
        }

        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        int quota = (int) Math.ceil((double) topK / groups.size());

        // 每组取 quota 条（已按分数排序）
        List<MilvusSearchClient.MilvusSearchResult> merged = new ArrayList<>();
        for (List<MilvusSearchClient.MilvusSearchResult> group : groups.values()) {
            merged.addAll(group.stream().limit(quota).toList());
        }

        // 全局按分数排序取 topK
        return merged.stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .toList();
    }

    // ---- 辅助方法 ----

    private CodeSearchData.SearchResult buildSearchResult(MilvusSearchClient.MilvusSearchResult candidate) {
        CodeSearchData.SearchResult result = new CodeSearchData.SearchResult();
        result.setId(candidate.getId());
        result.setNodeType(candidate.getNodeType());
        result.setFullName(candidate.getFullName());
        result.setName(extractName(candidate.getFullName()));
        result.setDigest(candidate.getDigest());
        result.setScore(candidate.getScore());
        return result;
    }

    private void enrichSearchResult(CodeSearchData.SearchResult searchResult, GraphQueryNode node) {
        Map<String, Object> props = node.getProperties();

        // is_library
        Object isLibraryObj = props.get("is_library");
        boolean isLibrary = isLibraryObj instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(isLibraryObj));
        searchResult.setIsLibrary(isLibrary);

        // source_code_location: 直接从节点属性获取 file_path（analyzer 阶段已写入）
        String filePath = getStringProp(props, "file_path", null);
        if (filePath != null && !filePath.isBlank()) {
            Object lineStart = props.get("line_start");
            Object lineEnd = props.get("line_end");
            if (lineStart != null && lineEnd != null) {
                searchResult.setSourceCodeLocation(filePath + ":" + lineStart + "-" + lineEnd);
            } else {
                searchResult.setSourceCodeLocation(filePath);
            }
        }

        // content: 仅库代码自动返回完整源码
        if (isLibrary) {
            Object contentObj = props.get("content");
            if (contentObj instanceof String encodedContent) {
                searchResult.setContent(decompressContent(encodedContent));
            }
        }

        // name: 优先用 properties 中的 name
        String nameFromProps = getStringProp(props, "name", null);
        if (nameFromProps != null && !nameFromProps.isBlank()) {
            searchResult.setName(nameFromProps);
        }
    }

    private CodeSearchData.SearchContext buildContext(String nodeId, int depth, List<String> edgeTypes) {
        try {
            int normalizedDepth = normalizeDepth(depth);

            // upstream (IN direction): edge.source → startNode, 取 source 作为上游
            GraphQuerySubgraph upstreamGraph = graphQueryDao.getSubgraph(nodeId, normalizedDepth, "IN", edgeTypes);
            List<CodeSearchData.ContextNode> upstream = toContextNodes(upstreamGraph, nodeId, true);

            // downstream (OUT direction): startNode → edge.target, 取 target 作为下游
            GraphQuerySubgraph downstreamGraph = graphQueryDao.getSubgraph(nodeId, normalizedDepth, "OUT", edgeTypes);
            List<CodeSearchData.ContextNode> downstream = toContextNodes(downstreamGraph, nodeId, false);

            CodeSearchData.SearchContext context = new CodeSearchData.SearchContext();
            context.setUpstream(upstream);
            context.setDownstream(downstream);
            return context;
        } catch (Exception e) {
            log.warn("Failed to build context for node {}: {}", nodeId, e.getMessage());
            return null;
        }
    }

    /**
     * @param subgraph  子图结果
     * @param startNodeId 起始节点 ID（排除自身）
     * @param useSource  true=取边的 source 作为上下文节点（IN/upstream），false=取 target（OUT/downstream）
     */
    private List<CodeSearchData.ContextNode> toContextNodes(GraphQuerySubgraph subgraph, String startNodeId, boolean useSource) {
        List<CodeSearchData.ContextNode> contextNodes = new ArrayList<>();
        Map<String, GraphQueryNode> nodeMap = new LinkedHashMap<>();
        for (GraphQueryNode node : subgraph.getNodes()) {
            nodeMap.put(node.getId(), node);
        }

        for (GraphQueryEdge edge : subgraph.getEdges()) {
            String relatedId = useSource ? edge.getSource() : edge.getTarget();
            // 排除起始节点自身
            if (relatedId.equals(startNodeId)) {
                continue;
            }
            GraphQueryNode relatedNode = nodeMap.get(relatedId);
            if (relatedNode == null) {
                continue;
            }

            Map<String, Object> props = relatedNode.getProperties();
            CodeSearchData.ContextNode cn = new CodeSearchData.ContextNode();
            cn.setId(relatedId);
            cn.setNodeType(getStringProp(props, "node_type", relatedNode.getTag()));
            cn.setFullName(getStringProp(props, "full_name", ""));
            cn.setEdgeType(edge.getType());
            cn.setEdgeProperties(edge.getProperties() != null ? new LinkedHashMap<>(edge.getProperties()) : Map.of());
            contextNodes.add(cn);
        }
        return contextNodes;
    }

    private String extractName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        int hashIdx = fullName.indexOf('#');
        if (hashIdx >= 0) {
            String methodPart = fullName.substring(hashIdx + 1);
            int parenIdx = methodPart.indexOf('(');
            return parenIdx >= 0 ? methodPart.substring(0, parenIdx) : methodPart;
        }
        int dotIdx = fullName.lastIndexOf('.');
        return dotIdx >= 0 ? fullName.substring(dotIdx + 1) : fullName;
    }

    private String buildSourceCodeLocation(Map<String, Object> props) {
        String filePath = getStringProp(props, "file_path", null);
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        Object lineStart = props.get("line_start");
        Object lineEnd = props.get("line_end");
        if (lineStart != null && lineEnd != null) {
            return filePath + ":" + lineStart + "-" + lineEnd;
        }
        return filePath;
    }

    private String buildSourceCodeLocationWithFallback(Map<String, Object> props, String nodeId) {
        String filePath = getStringProp(props, "file_path", null);
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        Object lineStart = props.get("line_start");
        Object lineEnd = props.get("line_end");
        if (lineStart != null && lineEnd != null) {
            return filePath + ":" + lineStart + "-" + lineEnd;
        }
        return filePath;
    }

    private Map<String, Integer> computeTypeDistribution(List<CodeSearchData.SearchResult> results) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (CodeSearchData.SearchResult r : results) {
            distribution.merge(r.getNodeType(), 1, Integer::sum);
        }
        return distribution;
    }

    private int countContextNodes(CodeSearchData.SearchContext context) {
        int count = 0;
        if (context.getUpstream() != null) {
            count += context.getUpstream().size();
        }
        if (context.getDownstream() != null) {
            count += context.getDownstream().size();
        }
        return count;
    }

    private int normalizeDepth(Integer depth) {
        if (depth == null || (depth <= 0 && depth != -1)) {
            return 1;
        }
        if (depth == -1) {
            return 10;
        }
        return depth;
    }

    private String decompressContent(String compressedBase64) {
        if (compressedBase64 == null || compressedBase64.isEmpty() || "null".equals(compressedBase64)) {
            return null;
        }
        String decompressed = ContentCompressor.decompress(compressedBase64);
        if (decompressed == null || decompressed.isEmpty()) {
            log.warn("Decompression returned empty content, fallback to raw string");
            return compressedBase64;
        }
        return decompressed;
    }

    private String getStringProp(Map<String, Object> props, String key, String defaultValue) {
        if (props == null) {
            return defaultValue;
        }
        Object value = props.get(key);
        if (value == null) {
            return defaultValue;
        }
        String str = String.valueOf(value);
        return str.isBlank() ? defaultValue : str;
    }
}
