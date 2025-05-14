package com.puti.code.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puti.code.server.dto.GraphEdge;
import com.puti.code.server.dto.GraphNode;
import com.puti.code.server.dto.GraphResponseMeta;
import com.puti.code.server.dto.NodeRequest;
import com.puti.code.server.dto.NodeResponse;
import com.puti.code.server.service.GraphQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GraphQueryController.class)
class GraphQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GraphQueryService graphQueryService;

    @Test
    void shouldReturnGenericGraphProtocol() throws Exception {
        GraphNode node = new GraphNode();
        node.setId("n1");
        node.setType("function");
        node.setLabel("createUser");
        node.setProperties(Map.of("full_name", "com.demo.UserService#createUser()"));

        GraphEdge edge = new GraphEdge();
        edge.setId("n1->n2:calls");
        edge.setSource("n1");
        edge.setTarget("n2");
        edge.setType("calls");
        edge.setCategory("SEMANTIC");
        edge.setProperties(Map.of("type", "calls", "category", "SEMANTIC", "line_number", 12));

        GraphResponseMeta meta = new GraphResponseMeta();
        meta.setNodeTypeStats(Map.of("function", 1));
        meta.setEdgeTypeStats(Map.of("calls", 1));
        meta.setLegend(Map.of("calls", "SEMANTIC"));
        meta.setEdgeTypeLabels(Map.of("calls", "调用关系"));
        meta.setEdgeCategoryLabels(Map.of("SEMANTIC", "语义"));
        meta.setQueryInfo(Map.of("queryType", "OUT", "pathDepth", 2));

        NodeResponse response = new NodeResponse();
        response.setNodes(List.of(node));
        response.setEdges(List.of(edge));
        response.setMeta(meta);

        when(graphQueryService.getNodes(any(NodeRequest.class))).thenReturn(response);

        NodeRequest request = new NodeRequest();
        request.setMethodFullName("com.demo.UserService#createUser()");
        request.setQueryType("downstream");
        request.setPathDepth(2);

        mockMvc.perform(post("/api/v1/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].id").value("n1"))
                .andExpect(jsonPath("$.nodes[0].type").value("function"))
                .andExpect(jsonPath("$.nodes[0].label").value("createUser"))
                .andExpect(jsonPath("$.nodes[0].properties.full_name").value("com.demo.UserService#createUser()"))
                .andExpect(jsonPath("$.edges[0].id").value("n1->n2:calls"))
                .andExpect(jsonPath("$.edges[0].type").value("calls"))
                .andExpect(jsonPath("$.edges[0].category").value("SEMANTIC"))
                .andExpect(jsonPath("$.edges[0].properties.line_number").value(12))
                .andExpect(jsonPath("$.meta.nodeTypeStats.function").value(1))
                .andExpect(jsonPath("$.meta.edgeTypeStats.calls").value(1))
                .andExpect(jsonPath("$.meta.legend.calls").value("SEMANTIC"))
                .andExpect(jsonPath("$.meta.edgeTypeLabels.calls").value("调用关系"))
                .andExpect(jsonPath("$.meta.edgeCategoryLabels.SEMANTIC").value("语义"));
    }
}
