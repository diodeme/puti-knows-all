package com.puti.code.server.dao;

import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphQueryDaoTest {

    @Test
    void shouldDelegateSubgraphQueryWithConvertedDirection() {
        GraphQueryRepository graphQueryRepository = mock(GraphQueryRepository.class);
        GraphQuerySubgraph subgraph = GraphQuerySubgraph.builder().build();
        when(graphQueryRepository.getSubgraph("node-1", 2, GraphDirection.OUT, null)).thenReturn(subgraph);

        GraphQueryDao dao = new GraphQueryDao(graphQueryRepository);
        dao.getSubgraph("node-1", 2, "OUT");

        verify(graphQueryRepository).getSubgraph("node-1", 2, GraphDirection.OUT, null);
    }

    @Test
    void shouldDelegateNodeDetailLookup() {
        GraphQueryRepository graphQueryRepository = mock(GraphQueryRepository.class);
        GraphQueryNode node = GraphQueryNode.builder().id("node-1").tag("function").build();
        when(graphQueryRepository.findNodeById("node-1")).thenReturn(Optional.of(node));

        GraphQueryDao dao = new GraphQueryDao(graphQueryRepository);
        dao.getNodeDetail("node-1");

        verify(graphQueryRepository).findNodeById("node-1");
    }
}
