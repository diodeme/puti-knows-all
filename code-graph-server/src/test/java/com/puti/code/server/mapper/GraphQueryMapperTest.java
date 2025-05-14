package com.puti.code.server.mapper;

import com.puti.code.server.dto.GraphEdge;
import com.puti.code.server.dto.GraphNode;
import com.puti.code.server.service.GraphQueryRecord;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQueryMapperTest {

    private final GraphQueryMapper mapper = Mappers.getMapper(GraphQueryMapper.class);

    @Test
    void shouldMapGraphNodeToGenericProtocol() {
        GraphQueryRecord.GraphNodeRecord record = new GraphQueryRecord.GraphNodeRecord(
                "node-1",
                "createUser",
                "com.demo.UserService#createUser()",
                "function",
                "public",
                false,
                true,
                false,
                Map.of("full_name", "com.demo.UserService#createUser()", "source_node", true));

        GraphNode node = mapper.toGraphNode(record);

        assertEquals("node-1", node.getId());
        assertEquals("function", node.getType());
        assertEquals("createUser", node.getLabel());
        assertEquals("com.demo.UserService#createUser()", node.getProperties().get("full_name"));
    }

    @Test
    void shouldMapGraphEdgeToGenericProtocol() {
        GraphQueryRecord.GraphEdgeRecord record = new GraphQueryRecord.GraphEdgeRecord(
                "a",
                "b",
                "injection_calls",
                "FRAMEWORK",
                Map.of("line_number", 10));

        GraphEdge edge = mapper.toGraphEdge(record);

        assertEquals("a", edge.getSource());
        assertEquals("b", edge.getTarget());
        assertEquals("injection_calls", edge.getType());
        assertEquals("FRAMEWORK", edge.getCategory());
        assertTrue(edge.getProperties().containsKey("line_number"));
        assertEquals("injection_calls", edge.getProperties().get("type"));
    }
}
