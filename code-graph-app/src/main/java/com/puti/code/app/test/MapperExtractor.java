package com.puti.code.app.test;

import com.google.common.collect.Sets;
import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.Relationship;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MapperExtractor implements Closeable {
    private final NebulaGraphClient nebulaGraphClient;
    private final AppConfig appConfig;

    public MapperExtractor() {
        nebulaGraphClient = new NebulaGraphClient();
        appConfig = AppConfig.getInstance();
    }

    @Override
    public void close() throws IOException {
        nebulaGraphClient.close();
    }

    /**
     * 方法调用链路数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodCallChain {
        /**
         * 方法ID
         */
        private String methodId;

        /**
         * 方法名称
         */
        private String methodName;

        /**
         * 调用链路（按调用顺序排列的方法ID列表）
         */
        private List<String> callChain;

        /**
         * 调用链路的方法名称（按调用顺序排列）
         */
        private List<String> callChainNames;

        /**
         * 调用深度
         */
        private int depth;

        public String callChain() {
            return toChain(callChain);
        }

        public static String toChain(List<String> chains){
            return String.join(" -> ", chains);
        }
    }

    public Set<String> queryMethod(String id) throws UnsupportedEncodingException {
        String query = """
                MATCH (v1:class) -[contains]-> (v2:function)
                WHERE v1.class.repo_id == "%s" and v1.class.branch_name == "%s" \
                AND id(v1) == "%s" \
                RETURN id(v2)
                """.formatted(appConfig.getProjectId(), appConfig.getBranch(), id);
        ResultSet resultSet = nebulaGraphClient.execute(query);
        int size = resultSet.rowsSize();
        Set<String> idList = Sets.newHashSet();
        for (int i = 0; i < size; i++) {
            ResultSet.Record valueWrappers = resultSet.rowValues(i);
            ValueWrapper valueWrapper = valueWrappers.get(0);
            idList.add(valueWrapper.asString());
        }
        return idList;
    }

    public Set<String> queryAllMethodId() {
        String query = """
                MATCH (v:class) \
                WHERE v.class.repo_id == "%s" and v.class.branch_name == "%s" \
                AND v.class.full_name ends with "Mapper" \
                RETURN v
                """.formatted(appConfig.getProjectId(), appConfig.getBranch());
        ResultSet resultSet = nebulaGraphClient.execute(query);

        if (!resultSet.isSucceeded()) {
            log.error("查询入口点失败: {}", resultSet.getErrorMessage());
            return Set.of();
        }
        Set<String> methodIdSet = Sets.newHashSet();
        // 解析查询结果
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            try {
                ValueWrapper nodeWrapper = resultSet.rowValues(i).get(0);
                if (nodeWrapper.isVertex()) {
                    Node node = nodeWrapper.asNode();
                    methodIdSet.addAll(queryMethod(node.getId().asString()));
                }
            } catch (Exception e) {
                log.warn("解析图谱数据时发生错误，跳过该记录", e);
            }
        }
        log.info("查询到 {} 个方法", methodIdSet.size());
        return methodIdSet;
    }

    /**
     * 查询方法调用链路并返回格式化结果
     *
     * @param methodIdSet 方法ID集合
     * @return 方法调用链路列表（过滤掉只有自己的方法）
     */
    public List<MethodCallChain> queryMethodTopCall(Set<String> methodIdSet) {
        String ids = methodIdSet.stream().map("""
                "%s"
                """::formatted).collect(Collectors.joining(","));
        String query = """
                GET SUBGRAPH WITH PROP 10 STEPS FROM %s IN calls,interface_calls,injection_calls \
                            YIELD VERTICES AS nodes, EDGES AS relationships
                """.formatted(ids);

        ResultSet resultSet = nebulaGraphClient.execute(query);

        if (!resultSet.isSucceeded()) {
            log.error("查询方法调用链路失败: {}", resultSet.getErrorMessage());
            return Collections.emptyList();
        }

        // 解析子图数据
        Map<String, String> methodIdToNameMap = new HashMap<>();
        Map<String, Set<String>> callRelations = new HashMap<>();

        // 解析结果
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            try {
                ResultSet.Record record = resultSet.rowValues(i);

                // 解析节点数据
                ValueWrapper nodesWrapper = record.get(0);
                if (nodesWrapper.isList()) {
                    for (ValueWrapper nodeWrapper : nodesWrapper.asList()) {
                        if (nodeWrapper.isVertex()) {
                            Node node = nodeWrapper.asNode();
                            String nodeId = node.getId().asString();

                            // 获取方法名称
                            String methodName = getMethodNameFromNode(node);
                            methodIdToNameMap.put(nodeId, methodName);
                        }
                    }
                }

                // 解析边数据
                ValueWrapper edgesWrapper = record.get(1);
                if (edgesWrapper.isList()) {
                    for (ValueWrapper edgeWrapper : edgesWrapper.asList()) {
                        if (edgeWrapper.isEdge()) {
                            Relationship relationship = edgeWrapper.asRelationship();
                            String sourceId = relationship.srcId().asString();
                            String targetId = relationship.dstId().asString();
                            String edgeType = relationship.edgeName();

                            callRelations.computeIfAbsent(targetId, k -> new HashSet<>()).add(sourceId);

                            // 调试信息
                            log.debug("发现调用关系: {} --[{}]--> {}",
                                    methodIdToNameMap.getOrDefault(sourceId, sourceId),
                                    edgeType,
                                    methodIdToNameMap.getOrDefault(targetId, targetId));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析子图数据时发生错误，跳过该记录", e);
            }
        }

        // 构建调用链路 - 注意：methodIdSet中的每个methodId都是入口方法
        List<MethodCallChain> callChains = new ArrayList<>();
        Set<String> callChain = Sets.newHashSet();
        for (String entryMethodId : methodIdSet) {
            // 检查这个入口方法是否有调用其他方法
            if (callRelations.containsKey(entryMethodId)) {
                // 构建该入口方法的所有可能调用路径
                List<List<String>> allPaths = buildAllCallPaths(entryMethodId, callRelations, new HashSet<>(), 10);

                for (List<String> path : allPaths) {
                    // 去掉重复的调用路径
                    if (callChain.contains(MethodCallChain.toChain(path))) {
                        continue;
                    }

                    // 过滤掉只有自己的方法（调用链路长度为1且只包含自己）
                    if (path.size() > 1) {
                        callChain.add(MethodCallChain.toChain(path));
                        List<String> callChainNames = path.stream()
                                .map(id -> methodIdToNameMap.getOrDefault(id, "Unknown Method [" + id + "]"))
                                .collect(Collectors.toList());

                        MethodCallChain methodCallChain = MethodCallChain.builder()
                                .methodId(entryMethodId)
                                .methodName(methodIdToNameMap.getOrDefault(entryMethodId, "Unknown Method [" + entryMethodId + "]"))
                                .callChain(path)
                                .callChainNames(callChainNames)
                                .depth(path.size() - 1)
                                .build();

                        callChains.add(methodCallChain);

                        // 记录调试信息
                        log.debug("入口方法 {} 的调用链路: {}",
                                methodIdToNameMap.getOrDefault(entryMethodId, entryMethodId),
                                String.join(" -> ", callChainNames));
                    } else {
                        log.debug("入口方法 {} 调用链路长度为1，跳过",
                                methodIdToNameMap.getOrDefault(entryMethodId, entryMethodId));
                    }
                }
            } else {
                log.debug("入口方法 {} 没有调用其他方法，跳过",
                        methodIdToNameMap.getOrDefault(entryMethodId, entryMethodId));
            }
        }

        log.info("成功构建 {} 个方法的调用链路", callChains.size());
        return callChains;
    }

    /**
     * 从节点中获取方法名称
     */
    private String getMethodNameFromNode(Node node) throws UnsupportedEncodingException {
        try {
            HashMap<String, ValueWrapper> properties = node.properties("function");
            // 尝试获取方法的全名
            if (properties.containsKey("full_name")) {
                return properties.get("full_name").asString();
            }
            // 如果没有全名，尝试获取方法名
            if (properties.containsKey("name")) {
                return properties.get("name").asString();
            }
            // 如果都没有，返回节点ID
            return node.getId().asString();
        } catch (Exception e) {
            log.warn("获取方法名称时发生错误，使用节点ID", e);
            return node.getId().asString();
        }
    }

    /**
     * 递归构建调用链路 - 深度优先遍历构建完整调用路径
     */
    private List<String> buildCallChain(String methodId, Map<String, Set<String>> callRelations, Set<String> visited) {
        List<String> chain = new ArrayList<>();
        chain.add(methodId);

        // 防止循环调用
        if (visited.contains(methodId)) {
            return chain;
        }

        Set<String> calledMethods = callRelations.get(methodId);
        if (calledMethods != null && !calledMethods.isEmpty()) {
            // 创建新的visited集合，避免影响其他分支
            Set<String> newVisited = new HashSet<>(visited);
            newVisited.add(methodId);

            // 对于每个被调用的方法，递归构建子链路
            for (String calledMethod : calledMethods) {
                List<String> subChain = buildCallChain(calledMethod, callRelations, newVisited);
                // 将子链路添加到当前链中（去掉重复的起始节点）
                if (subChain.size() > 1) {
                    chain.addAll(subChain.subList(1, subChain.size()));
                    // 只取第一个调用路径，避免链路过于复杂
                    break;
                }
            }
        }

        return chain;
    }

    /**
     * 构建所有可能的调用路径（用于复杂场景）
     */
    private List<List<String>> buildAllCallPaths(String methodId, Map<String, Set<String>> callRelations,
                                                 Set<String> visited, int maxDepth) {
        List<List<String>> allPaths = new ArrayList<>();

        // 防止无限递归
        if (visited.contains(methodId) || maxDepth <= 0) {
            List<String> singlePath = new ArrayList<>();
            singlePath.add(methodId);
            allPaths.add(singlePath);
            return allPaths;
        }

        Set<String> calledMethods = callRelations.get(methodId);
        if (calledMethods == null || calledMethods.isEmpty()) {
            // 叶子节点
            List<String> singlePath = new ArrayList<>();
            singlePath.add(methodId);
            allPaths.add(singlePath);
        } else {
            // 有调用关系的节点
            Set<String> newVisited = new HashSet<>(visited);
            newVisited.add(methodId);

            for (String calledMethod : calledMethods) {
                List<List<String>> subPaths = buildAllCallPaths(calledMethod, callRelations, newVisited, maxDepth - 1);
                for (List<String> subPath : subPaths) {
                    List<String> fullPath = new ArrayList<>();
                    fullPath.add(methodId);
                    fullPath.addAll(subPath);
                    allPaths.add(fullPath);
                }
            }
        }

        return allPaths;
    }

    /**
     * 将调用链路导出为Excel文件
     *
     * @param callChains 调用链路列表
     * @param filePath   Excel文件路径
     */
    public void exportToExcel(List<MethodCallChain> callChains, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("方法调用链路");

            // 创建样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            Cell cell0 = headerRow.createCell(0);
            cell0.setCellValue("序号");
            cell0.setCellStyle(headerStyle);

            Cell cell1 = headerRow.createCell(1);
            cell1.setCellValue("入口方法ID");
            cell1.setCellStyle(headerStyle);

            Cell cell2 = headerRow.createCell(2);
            cell2.setCellValue("入口方法名称");
            cell2.setCellStyle(headerStyle);

            Cell cell3 = headerRow.createCell(3);
            cell3.setCellValue("调用深度");
            cell3.setCellStyle(headerStyle);

            Cell cell4 = headerRow.createCell(4);
            cell4.setCellValue("完整调用链路");
            cell4.setCellStyle(headerStyle);

            // 找出最大调用深度，用于创建足够的列
            int maxDepth = callChains.stream()
                    .mapToInt(chain -> chain.getCallChain().size())
                    .max()
                    .orElse(0);

            // 为调用链路的每一步创建列标题
            for (int i = 0; i < maxDepth; i++) {
                Cell cellHeader = headerRow.createCell(5 + i);
                cellHeader.setCellValue("调用节点" + (i + 1));
                cellHeader.setCellStyle(headerStyle);
            }

            // 创建数据行
            int rowNum = 1;
            for (MethodCallChain callChain : callChains) {
                Row row = sheet.createRow(rowNum);

                row.createCell(0).setCellValue(rowNum); // 序号
                row.createCell(1).setCellValue(callChain.getMethodId());
                row.createCell(2).setCellValue(callChain.getMethodName());
                row.createCell(3).setCellValue(callChain.getDepth());

                // 完整调用链路（用箭头连接）
                String fullChain = String.join(" -> ", callChain.getCallChainNames());
                row.createCell(4).setCellValue(fullChain);

                // 填充调用链路的每个节点
                List<String> chainNames = callChain.getCallChainNames();
                for (int i = 0; i < chainNames.size() && i < maxDepth; i++) {
                    row.createCell(5 + i).setCellValue(chainNames.get(i));
                }

                rowNum++;
            }

            // 自动调整列宽
            for (int i = 0; i < 5 + maxDepth; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小列宽
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                log.info("成功导出 {} 个方法调用链路到文件: {}", callChains.size(), filePath);
            }

        } catch (IOException e) {
            log.error("导出Excel文件时发生错误: {}", filePath, e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    public static void main(String[] args) throws IOException {
        try (MapperExtractor extractor = new MapperExtractor()) {

            // 查询所有Mapper方法
            Set<String> allMethodId = extractor.queryAllMethodId();
            log.info("找到 {} 个Mapper方法", allMethodId.size());

            // 获取调用链路
            List<MethodCallChain> callChains = extractor.queryMethodTopCall(allMethodId);
            log.info("构建了 {} 个有效的调用链路", callChains.size());

            // 导出到Excel
            if (!callChains.isEmpty()) {
                String filePath = "method_call_chains.xlsx";
                extractor.exportToExcel(callChains, filePath);

                // 打印一些统计信息
                log.info("调用链路统计:");
                log.info("- 总方法数: {}", callChains.size());
                log.info("- 平均调用深度: {}",
                        callChains.stream().mapToInt(MethodCallChain::getDepth).average().orElse(0.0));
                log.info("- 最大调用深度: {}",
                        callChains.stream().mapToInt(MethodCallChain::getDepth).max().orElse(0));
            } else {
                log.info("没有找到有效的调用链路");
            }
        }
    }
}
