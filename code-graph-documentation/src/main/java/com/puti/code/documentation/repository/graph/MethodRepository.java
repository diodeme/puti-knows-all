package com.puti.code.documentation.repository.graph;

import com.puti.code.base.util.ContentCompressor;
import com.puti.code.documentation.controller.response.MethodContent;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 方法信息图数据库Repository
 * 
 * @author diodehe
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MethodRepository {

    private final GraphQueryRepository graphQueryRepository;
    
    /**
     * 根据方法ID查询方法内容
     * 
     * @param methodId 方法ID
     * @return 方法内容
     */
    public MethodContent getMethodContent(String methodId) {
        try {
            log.info("开始查询方法内容，方法ID: {}", methodId);
            return graphQueryRepository.findNodeById(methodId)
                    .map(node -> parseMethodContent(methodId, node))
                    .orElse(null);
            
        } catch (Exception e) {
            log.error("查询方法内容时发生错误，方法ID: {}", methodId, e);
            return null;
        }
    }
    
    /**
     * 解析方法内容查询结果
     */
    private MethodContent parseMethodContent(String methodId, GraphQueryNode node) {
        try {
            String fullName = getStringValue(node, "full_name");
            String methodName = getStringValue(node, "name");
            String compressedContent = getStringValue(node, "content");
            Boolean isEntryPoint = getBooleanValue(node, "is_entry_point");

            // 解压缩内容
            String content = null;
            if (compressedContent != null && !compressedContent.isEmpty()) {
                try {
                    content = ContentCompressor.decompress(compressedContent);
                } catch (Exception e) {
                    log.warn("解压缩方法内容失败，方法ID: {}, 错误: {}", methodId, e.getMessage());
                    content = compressedContent; // 如果解压失败，使用原始内容
                }
            }

            return MethodContent.builder()
                    .methodId(methodId)
                    .fullName(fullName)
                    .methodName(methodName)
                    .content(content)
                    .isEntryPoint(isEntryPoint)
                    .build();

        } catch (Exception e) {
            log.error("解析方法内容时发生错误，方法ID: {}", methodId, e);
            return null;
        }
    }
    
    /**
     * 从结果集中获取字符串值
     */
    private String getStringValue(GraphQueryNode node, String key) {
        try {
            Object value = node.getProperties().get(key);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            log.debug("获取字符串值失败，key: {}, 错误: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 从结果集中获取布尔值
     */
    private Boolean getBooleanValue(GraphQueryNode node, String key) {
        try {
            Object value = node.getProperties().get(key);
            if (value == null) {
                return null;
            }
            return value instanceof Boolean booleanValue ? booleanValue : Boolean.parseBoolean(String.valueOf(value));
        } catch (Exception e) {
            log.debug("获取布尔值失败，key: {}, 错误: {}", key, e.getMessage());
            return null;
        }
    }
}
