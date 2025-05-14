package com.puti.code.server.mapper;

import com.puti.code.server.dto.GraphEdge;
import com.puti.code.server.dto.GraphNode;
import com.puti.code.server.dto.MethodSearchResult;
import com.puti.code.server.service.GraphQueryRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface GraphQueryMapper {

    @Mapping(target = "nodeId", source = "id")
    @Mapping(target = "id", source = "id")
    MethodSearchResult toMethodSearchResult(GraphQueryRecord.MethodSearchRecord record);

    default GraphNode toGraphNode(GraphQueryRecord.GraphNodeRecord record) {
        GraphNode node = new GraphNode();
        node.setId(record.id());
        node.setType(record.type());
        node.setLabel(record.name());
        node.setProperties(new LinkedHashMap<>(record.properties()));
        return node;
    }

    default GraphEdge toGraphEdge(GraphQueryRecord.GraphEdgeRecord record) {
        GraphEdge edge = new GraphEdge();
        edge.setId(record.source() + "->" + record.target() + ":" + record.type());
        edge.setSource(record.source());
        edge.setTarget(record.target());
        edge.setType(record.type());
        edge.setCategory(record.category());
        Map<String, Object> properties = new LinkedHashMap<>();
        if (record.properties() != null) {
            properties.putAll(record.properties());
        }
        properties.putIfAbsent("type", record.type());
        properties.putIfAbsent("category", record.category());
        edge.setProperties(properties);
        return edge;
    }
}
