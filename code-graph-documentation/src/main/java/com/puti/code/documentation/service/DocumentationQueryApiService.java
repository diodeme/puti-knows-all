package com.puti.code.documentation.service;

import com.puti.code.documentation.controller.response.*;
import com.puti.code.documentation.dto.PageRequest;
import com.puti.code.documentation.dto.PageResult;
import com.puti.code.documentation.repository.graph.MethodRepository;
import com.puti.code.documentation.repository.sql.mapper.AggregatedDocumentationMapper;
import com.puti.code.documentation.repository.sql.mapper.DocumentationAggregationMapper;
import com.puti.code.documentation.repository.sql.mapper.DocumentationMapper;
import com.puti.code.documentation.repository.sql.mapper.DocumentationMethodMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 说明书查询API服务
 * 
 * @author diodehe
 */
@Slf4j
@Service
public class DocumentationQueryApiService {
    
    @Autowired
    private AggregatedDocumentationMapper aggregatedDocumentationMapper;
    
    @Autowired
    private DocumentationAggregationMapper documentationAggregationMapper;
    
    @Autowired
    private DocumentationMapper documentationMapper;
    
    @Autowired
    private DocumentationMethodMapper documentationMethodMapper;
    
    @Autowired
    private MethodRepository methodRepository;
    
    /**
     * 查询所有已接入的子系统
     */
    public List<ProjectInfo> getAllProjects() {
        try {
            log.info("开始查询所有已接入的子系统");
            List<ProjectInfo> projects = aggregatedDocumentationMapper.findAllProjects();
            log.info("查询到 {} 个子系统", projects.size());
            return projects;
        } catch (Exception e) {
            log.error("查询所有子系统时发生错误", e);
            throw new RuntimeException("查询子系统失败", e);
        }
    }
    
    /**
     * 查询子系统下所有聚合说明书
     */
    public List<AggregatedDocumentationSummary> getAggregatedDocumentationsByProject(String projectId) {
        try {
            log.info("开始查询项目 {} 下的聚合说明书", projectId);
            List<AggregatedDocumentationSummary> summaries = 
                    aggregatedDocumentationMapper.findSummariesByProjectId(projectId);
            log.info("查询到 {} 个聚合说明书", summaries.size());
            return summaries;
        } catch (Exception e) {
            log.error("查询项目 {} 的聚合说明书时发生错误", projectId, e);
            throw new RuntimeException("查询聚合说明书失败", e);
        }
    }
    
    /**
     * 查询指定聚合说明书下所有流程说明书
     */
    public List<ProcessDocumentationSummary> getProcessDocumentationsByAggregated(String aggregatedDocumentationId) {
        try {
            log.info("开始查询聚合说明书 {} 下的流程说明书", aggregatedDocumentationId);
            List<ProcessDocumentationSummary> summaries = 
                    documentationAggregationMapper.findProcessDocumentationsByAggregatedId(aggregatedDocumentationId);
            log.info("查询到 {} 个流程说明书", summaries.size());
            return summaries;
        } catch (Exception e) {
            log.error("查询聚合说明书 {} 的流程说明书时发生错误", aggregatedDocumentationId, e);
            throw new RuntimeException("查询流程说明书失败", e);
        }
    }
    
    /**
     * 根据聚合说明书id查询说明书内容
     */
    public String getAggregatedDocumentationContent(String aggregatedDocumentationId) {
        try {
            log.info("开始查询聚合说明书 {} 的内容", aggregatedDocumentationId);
            String content = aggregatedDocumentationMapper.findContentById(aggregatedDocumentationId);
            if (content == null) {
                log.warn("未找到聚合说明书 {} 的内容", aggregatedDocumentationId);
                throw new RuntimeException("聚合说明书不存在");
            }
            log.info("成功查询到聚合说明书 {} 的内容，长度: {}", aggregatedDocumentationId, content.length());
            return content;
        } catch (Exception e) {
            log.error("查询聚合说明书 {} 内容时发生错误", aggregatedDocumentationId, e);
            throw new RuntimeException("查询聚合说明书内容失败", e);
        }
    }
    
    /**
     * 根据流程说明书id查询说明书内容
     */
    public String getProcessDocumentationContent(String documentationId) {
        try {
            log.info("开始查询流程说明书 {} 的内容", documentationId);
            String content = documentationMapper.findContentById(documentationId);
            if (content == null) {
                log.warn("未找到流程说明书 {} 的内容", documentationId);
                throw new RuntimeException("流程说明书不存在");
            }
            log.info("成功查询到流程说明书 {} 的内容，长度: {}", documentationId, content.length());
            return content;
        } catch (Exception e) {
            log.error("查询流程说明书 {} 内容时发生错误", documentationId, e);
            throw new RuntimeException("查询流程说明书内容失败", e);
        }
    }
    
    /**
     * 根据流程说明书id查询关联的所有方法id，支持分页
     */
    public PageResult<String> getMethodIdsByDocumentation(String documentationId, PageRequest pageRequest) {
        try {
            log.info("开始分页查询流程说明书 {} 的方法ID，页码: {}, 每页大小: {}", 
                    documentationId, pageRequest.getPage(), pageRequest.getSize());
            
            // 查询总数
            long total = documentationMethodMapper.countByDocumentationId(documentationId);
            if (total == 0) {
                log.info("流程说明书 {} 没有关联的方法", documentationId);
                return PageResult.empty(pageRequest);
            }
            
            // 分页查询方法ID
            List<String> methodIds = documentationMethodMapper.findMethodIdsByDocumentationIdWithPage(
                    documentationId, pageRequest.getLimit(), pageRequest.getOffset());
            
            log.info("查询到 {} 个方法ID，总数: {}", methodIds.size(), total);
            return PageResult.of(methodIds, pageRequest, total);
            
        } catch (Exception e) {
            log.error("分页查询流程说明书 {} 的方法ID时发生错误", documentationId, e);
            throw new RuntimeException("查询方法ID失败", e);
        }
    }
    
    /**
     * 根据方法id查询方法内容
     */
    public MethodContent getMethodContent(String methodId) {
        try {
            log.info("开始查询方法 {} 的内容", methodId);
            MethodContent methodContent = methodRepository.getMethodContent(methodId);
            if (methodContent == null) {
                log.warn("未找到方法 {} 的内容", methodId);
                throw new RuntimeException("方法不存在");
            }
            log.info("成功查询到方法 {} 的内容", methodId);
            return methodContent;
        } catch (Exception e) {
            log.error("查询方法 {} 内容时发生错误", methodId, e);
            throw new RuntimeException("查询方法内容失败", e);
        }
    }
}
