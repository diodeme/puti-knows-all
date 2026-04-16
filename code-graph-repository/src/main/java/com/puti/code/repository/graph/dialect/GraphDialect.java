package com.puti.code.repository.graph.dialect;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgePropertySchema;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.query.GraphDirection;

import java.util.List;
import java.util.Map;

/**
 * 图后端方言抽象，负责把逻辑意图翻译成底层查询/DDL。
 */
public interface GraphDialect {

    String buildUseSpaceStatement(String spaceName);

    String buildShowEdgesQuery();

    String buildDescribeEdgeQuery(String edgeName);

    String buildShowCreateEdgeQuery(String edgeName);

    String buildCreateEdgeStatement(EdgeSchema schema);

    String buildAlterEdgeAddPropertiesStatement(String edgeName, List<EdgePropertySchema> missingProperties);

    // Tag DDL
    String buildShowTagsQuery();

    String buildCreateTagStatement(String tagName, Map<String, String> properties);

    // Tag Index DDL
    String buildCreateTagIndexStatement(String tagName, String propName, int stringLength);

    String buildRebuildTagIndexStatement(String tagName, String propName);

    String buildInsertNodeStatement(Node node);

    String buildInsertEdgeStatement(Edge edge);

    String buildBatchInsertNodesStatement(String tag, List<Node> nodes);

    String buildBatchInsertEdgesStatement(String edgeType, List<String> propertyNames, List<Edge> edges);

    String buildSearchMethodByNameQuery(String methodName);

    String buildFindFunctionByFullNameQuery(String methodFullName);

    String buildFindFunctionIdByFullNameQuery(String methodFullName);

    String buildGetSubgraphQuery(String startNodeId, int pathDepth, GraphDirection direction, List<String> edgeTypes);

    String buildFindNodeByIdQuery(String nodeId);

    String buildGetEntryPointsQuery(String projectId, String branchName);

    String buildCountEntryPointsQuery();
}
