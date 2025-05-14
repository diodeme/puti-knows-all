package com.puti.code.repository.graph.file;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.model.FunctionNode;
import com.puti.code.base.model.NodeType;
import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileGraphQueryRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadFunctionsAndSubgraphFromLocalFiles() {
        Path nodeFile = tempDir.resolve("nodes.jsonl");
        Path edgeFile = tempDir.resolve("edges.jsonl");
        LocalFileGraphStorageRepository storageRepository = new LocalFileGraphStorageRepository(tempDir);
        try {
            FunctionNode source = FunctionNode.builder()
                    .id("function-1")
                    .nodeType(NodeType.FUNCTION)
                    .name("findUser")
                    .fullName("com.demo.UserService#findUser")
                    .visibility("public")
                    .isEntryPoint(true)
                    .repoId("demo")
                    .branchName("main")
                    .content("source")
                    .build();
            FunctionNode target = FunctionNode.builder()
                    .id("function-2")
                    .nodeType(NodeType.FUNCTION)
                    .name("loadUser")
                    .fullName("com.demo.UserDao#loadUser")
                    .visibility("public")
                    .content("target")
                    .build();
            Edge edge = Edge.builder()
                    .srcId("function-1")
                    .dstId("function-2")
                    .type(EdgeType.CALLS)
                    .lineNumber(18)
                    .build();

            storageRepository.insertNode(source);
            storageRepository.insertNode(target);
            storageRepository.insertEdge(edge);
        } finally {
            storageRepository.close();
        }

        LocalFileGraphStore graphStore = new LocalFileGraphStore(nodeFile, edgeFile);
        LocalFileGraphQueryRepository queryRepository = new LocalFileGraphQueryRepository(graphStore);

        assertEquals(1, queryRepository.searchMethodByName("findUser").size());
        assertTrue(queryRepository.findFunctionByFullName("com.demo.UserService#findUser").isPresent());
        assertTrue(queryRepository.findNodeById("function-1").isPresent());
        assertEquals(1, queryRepository.getEntryPoints("demo", "main").size());
        assertEquals(1L, queryRepository.countEntryPoints());

        GraphQuerySubgraph subgraph = queryRepository.getSubgraph("function-1", 1, GraphDirection.OUT, List.of("calls"));
        assertEquals(2, subgraph.getNodes().size());
        assertEquals(1, subgraph.getEdges().size());
        assertEquals("function-2", subgraph.getEdges().get(0).getTarget());
        assertEquals(18, subgraph.getEdges().get(0).getProperties().get("line_number"));
    }
}
