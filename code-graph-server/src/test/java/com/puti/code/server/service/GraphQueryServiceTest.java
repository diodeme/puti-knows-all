package com.puti.code.server.service;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import com.puti.code.server.dao.GraphQueryDao;
import com.puti.code.server.dto.MethodSearchResult;
import com.puti.code.server.mapper.GraphQueryMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphQueryServiceTest {

    private final GraphQueryService service = new GraphQueryService(mock(GraphQueryDao.class), mock(GraphQueryMapper.class));

    @Test
    void shouldUseRegistryDisplayNameForDynamicEdgeTypeLabel() {
        EdgeSchemaRegistry.getInstance().register(EdgeSchema.builder()
                .value("custom_flow")
                .category(EdgeCategory.DATA_FLOW)
                .displayName("自定义流转")
                .comment("custom flow")
                .build());
        GraphQueryService queryService = new GraphQueryService(mock(GraphQueryDao.class), mock(GraphQueryMapper.class));

        var response = new com.puti.code.server.dto.NodeResponse();
        var edge = new com.puti.code.server.dto.GraphEdge();
        edge.setType("custom_flow");
        edge.setCategory("DATA_FLOW");
        edge.setProperties(Map.of("type", "custom_flow", "category", "DATA_FLOW"));
        response.setNodes(List.of());
        response.setEdges(List.of(edge));

        queryService.enrichResponseMeta(response, Map.of("queryType", "OUT"));

        assertNotNull(response.getMeta());
        assertEquals("自定义流转", response.getMeta().getEdgeTypeLabels().get("custom_flow"));
        assertEquals("数据流", response.getMeta().getEdgeCategoryLabels().get("DATA_FLOW"));
    }

    @Test
    void shouldPreferCategoryAlreadyPresentInEdgeProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("category", "FRAMEWORK");

        assertEquals("FRAMEWORK", service.resolveEdgeCategory("custom_flow", properties));
    }

    @Test
    void shouldMapGenericNodeToGraphNodeRecord() {
        GraphQueryNode node = GraphQueryNode.builder()
                .id("class-1")
                .tag("class")
                .properties(new LinkedHashMap<>(Map.of(
                        "name", "UserService",
                        "full_name", "com.demo.UserService",
                        "is_library", false)))
                .build();

        GraphQueryRecord.GraphNodeRecord record = service.toGraphNodeRecord(node, "class-1");

        assertNotNull(record);
        assertEquals("class-1", record.id());
        assertEquals("UserService", record.name());
        assertEquals("com.demo.UserService", record.fullName());
        assertEquals("class", record.type());
        assertEquals(Boolean.TRUE, record.sourceNode());
        assertEquals("class", record.properties().get("node_type"));
        assertEquals("UserService", record.properties().get("label"));
    }

    @Test
    void shouldNormalizeAnnotationTagToStableNodeType() {
        GraphQueryNode node = GraphQueryNode.builder()
                .id("annotation-1")
                .tag("annotations")
                .properties(new LinkedHashMap<>(Map.of(
                        "name", "Transactional",
                        "full_name", "com.demo.UserService#Transactional",
                        "type", "ANNOTATION")))
                .build();

        GraphQueryRecord.GraphNodeRecord record = service.toGraphNodeRecord(node, "method-1");

        assertNotNull(record);
        assertEquals("annotation", record.type());
        assertEquals("annotation", record.properties().get("node_type"));
        assertEquals("annotations", record.properties().get("tag"));
    }

    @Test
    void shouldNormalizeMarkerAnnotationTagToStableNodeType() {
        GraphQueryNode node = GraphQueryNode.builder()
                .id("marker-annotation-1")
                .tag("marker_annotations")
                .properties(new LinkedHashMap<>(Map.of(
                        "name", "Override",
                        "full_name", "com.demo.UserService#Override")))
                .build();

        GraphQueryRecord.GraphNodeRecord record = service.toGraphNodeRecord(node, "method-1");

        assertNotNull(record);
        assertEquals("marker_annotation", record.type());
        assertEquals("marker_annotation", record.properties().get("node_type"));
        assertEquals("marker_annotations", record.properties().get("tag"));
    }

    @Test
    void shouldKeepMetaQueryInfoWhenSubgraphRootCannotBeResolved() {
        GraphQueryDao graphQueryDao = mock(GraphQueryDao.class);
        GraphQueryMapper mapper = mock(GraphQueryMapper.class);
        when(graphQueryDao.findFunctionIdByFullName("com.demo.UserService#findUser")).thenReturn(Optional.empty());
        GraphQueryService queryService = new GraphQueryService(graphQueryDao, mapper);

        var request = new com.puti.code.server.dto.NodeRequest();
        request.setQueryType("downstream");
        request.setMethodFullName("com.demo.UserService#findUser");
        request.setPathDepth(3);

        var response = queryService.getNodes(request);

        assertNotNull(response.getMeta());
        assertEquals("OUT", response.getMeta().getQueryInfo().get("queryType"));
        assertEquals(3, response.getMeta().getQueryInfo().get("pathDepth"));
        assertEquals(3, response.getMeta().getQueryInfo().get("resolvedPathDepth"));
        assertEquals("com.demo.UserService#findUser", response.getMeta().getQueryInfo().get("methodFullName"));
    }

    @Test
    void shouldPreserveAllDepthRequestInMetaWhileResolvingEffectiveDepth() {
        GraphQueryDao graphQueryDao = mock(GraphQueryDao.class);
        GraphQueryMapper mapper = mock(GraphQueryMapper.class);
        when(graphQueryDao.findFunctionIdByFullName("com.demo.UserService#findUser")).thenReturn(Optional.empty());
        GraphQueryService queryService = new GraphQueryService(graphQueryDao, mapper);

        var request = new com.puti.code.server.dto.NodeRequest();
        request.setQueryType("downstream");
        request.setMethodFullName("com.demo.UserService#findUser");
        request.setPathDepth(-1);

        var response = queryService.getNodes(request);

        assertNotNull(response.getMeta());
        assertEquals("OUT", response.getMeta().getQueryInfo().get("queryType"));
        assertEquals(-1, response.getMeta().getQueryInfo().get("pathDepth"));
        assertEquals(10, response.getMeta().getQueryInfo().get("resolvedPathDepth"));
        assertEquals("com.demo.UserService#findUser", response.getMeta().getQueryInfo().get("methodFullName"));
    }

    @Test
    void shouldSearchMethodsThroughGenericRepositoryResult() {
        GraphQueryDao graphQueryDao = mock(GraphQueryDao.class);
        GraphQueryMapper mapper = mock(GraphQueryMapper.class);
        GraphQueryNode node = GraphQueryNode.builder()
                .id("method-1")
                .tag("function")
                .properties(new LinkedHashMap<>(Map.of(
                        "name", "findUser",
                        "full_name", "com.demo.UserService#findUser",
                        "visibility", "public")))
                .build();
        MethodSearchResult searchResult = new MethodSearchResult();
        searchResult.setId("method-1");
        searchResult.setNodeId("method-1");
        searchResult.setFullName("com.demo.UserService#findUser");
        when(graphQueryDao.searchMethodByName("findUser")).thenReturn(List.of(node));
        when(mapper.toMethodSearchResult(any())).thenReturn(searchResult);

        GraphQueryService queryService = new GraphQueryService(graphQueryDao, mapper);
        List<MethodSearchResult> results = queryService.searchMethodByName("findUser");

        assertEquals(1, results.size());
        assertEquals("method-1", results.get(0).getId());
    }

    @Test
    void shouldBuildSubgraphResponseFromGenericGraphData() {
        GraphQueryDao graphQueryDao = mock(GraphQueryDao.class);
        GraphQueryMapper mapper = mock(GraphQueryMapper.class);
        GraphQueryNode root = GraphQueryNode.builder()
                .id("method-1")
                .tag("function")
                .properties(new LinkedHashMap<>(Map.of(
                        "name", "findUser",
                        "full_name", "com.demo.UserService#findUser")))
                .build();
        GraphQueryNode downstream = GraphQueryNode.builder()
                .id("method-2")
                .tag("function")
                .properties(new LinkedHashMap<>(Map.of(
                        "name", "loadUser",
                        "full_name", "com.demo.UserDao#loadUser")))
                .build();
        GraphQueryEdge edge = GraphQueryEdge.builder()
                .source("method-1")
                .target("method-2")
                .type("custom_flow")
                .properties(new LinkedHashMap<>(Map.of("line_number", 12)))
                .build();
        GraphQuerySubgraph subgraph = GraphQuerySubgraph.builder()
                .nodes(List.of(root, downstream))
                .edges(List.of(edge))
                .build();

        when(graphQueryDao.findFunctionIdByFullName("com.demo.UserService#findUser")).thenReturn(Optional.of("method-1"));
        when(graphQueryDao.getSubgraph("method-1", 2, "OUT")).thenReturn(subgraph);
        when(mapper.toGraphNode(any())).thenAnswer(invocation -> {
            GraphQueryRecord.GraphNodeRecord record = invocation.getArgument(0);
            var graphNode = new com.puti.code.server.dto.GraphNode();
            graphNode.setId(record.id());
            graphNode.setType(record.type());
            graphNode.setLabel(record.name());
            graphNode.setProperties(record.properties());
            return graphNode;
        });
        when(mapper.toGraphEdge(any())).thenAnswer(invocation -> {
            GraphQueryRecord.GraphEdgeRecord record = invocation.getArgument(0);
            var graphEdge = new com.puti.code.server.dto.GraphEdge();
            graphEdge.setId(record.source() + "->" + record.target() + ":" + record.type());
            graphEdge.setSource(record.source());
            graphEdge.setTarget(record.target());
            graphEdge.setType(record.type());
            graphEdge.setCategory(record.category());
            graphEdge.setProperties(record.properties());
            return graphEdge;
        });

        GraphQueryService queryService = new GraphQueryService(graphQueryDao, mapper);
        var request = new com.puti.code.server.dto.NodeRequest();
        request.setQueryType("downstream");
        request.setMethodFullName("com.demo.UserService#findUser");
        request.setPathDepth(2);

        var response = queryService.getNodes(request);

        assertEquals(2, response.getNodes().size());
        assertEquals(1, response.getEdges().size());
        assertEquals("OUT", response.getMeta().getQueryInfo().get("queryType"));
        assertEquals(1, response.getMeta().getEdgeTypeStats().get("custom_flow"));
        assertEquals("数据流", response.getMeta().getEdgeCategoryLabels().get("DATA_FLOW"));
    }
}
