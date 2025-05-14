package com.puti.code.documentation.service;

import com.puti.code.base.entity.rdb.AggregatedDocumentation;
import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.entity.rdb.DocumentationMethod;
import com.puti.code.documentation.controller.response.AggregatedDocumentationResponse;
import com.puti.code.documentation.controller.response.ProcessDocumentationResponse;
import com.puti.code.documentation.repository.sql.AggregatedDocumentationRepository;
import com.puti.code.documentation.repository.sql.DocumentationMethodRepository;
import com.puti.code.documentation.repository.sql.DocumentationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 说明书查询服务
 *
 * @author diodehe
 */
@Slf4j
@Service
public class DocumentationQueryService {

    @Autowired
    private AggregatedDocumentationRepository aggregatedDocumentationRepository;

    @Autowired
    private DocumentationRepository documentationRepository;

    @Autowired
    private DocumentationMethodRepository documentationMethodRepository;

    /**
     * 批量查询聚合说明书
     *
     * @param documentIds 说明书ID列表
     * @return 聚合说明书响应DTO列表
     */
    public List<AggregatedDocumentationResponse> getAggregatedDocumentationBatch(List<String> documentIds) {
        try {
            log.info("批量查询聚合说明书，ID数量: {}", documentIds.size());

            // 使用批量查询，一次性查询所有记录
            List<AggregatedDocumentation> aggregatedDocs =
                    aggregatedDocumentationRepository.findByIds(documentIds);

            // 转换为响应DTO
            List<AggregatedDocumentationResponse> responses = aggregatedDocs.stream()
                    .map(doc -> AggregatedDocumentationResponse.builder()
                            .id(doc.getId())
                            .content(doc.getContent())
                            .build())
                    .collect(Collectors.toList());

            log.info("批量查询聚合说明书完成，成功查询: {}, 总请求: {}", responses.size(), documentIds.size());
            return responses;

        } catch (Exception e) {
            log.error("批量查询聚合说明书时发生错误，ID列表: {}", documentIds, e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量查询流程说明书及其关联方法
     *
     * @param documentIds 说明书ID列表
     * @return 流程说明书响应DTO列表
     */
    public List<ProcessDocumentationResponse> getProcessDocumentationBatch(List<String> documentIds) {
        try {
            log.info("批量查询流程说明书，ID数量: {}", documentIds.size());

            // 批量查询说明书基本信息
            List<Documentation> documentations = documentationRepository.findByIds(documentIds);

            if (documentations.isEmpty()) {
                log.warn("未找到任何流程说明书，ID列表: {}", documentIds);
                return new ArrayList<>();
            }

            // 批量查询关联的方法信息
//            List<DocumentationMethod> allMethods = documentationMethodRepository.findByDocumentationIds(documentIds);

            // 按说明书ID分组方法信息
//            Map<String, List<DocumentationMethod>> methodsByDocId = allMethods.stream()
//                    .collect(Collectors.groupingBy(DocumentationMethod::getDocumentationId));

            // 构建响应列表
            List<ProcessDocumentationResponse> responses = documentations.stream()
                    .map(documentation -> {
//                        // 获取该说明书的方法列表
//                        List<DocumentationMethod> methods = methodsByDocId.getOrDefault(
//                                documentation.getId(), new ArrayList<>());
//
//                        // 转换方法信息
//                        List<ProcessDocumentationResponse.MethodInfo> methodInfos = methods.stream()
//                                .map(this::convertToMethodInfo)
//                                .collect(Collectors.toList());

                        return ProcessDocumentationResponse.builder()
                                .id(documentation.getId())
                                .content(documentation.getContent())
                                .entryPointId(documentation.getEntryPointId())
                                .entryPointName(documentation.getEntryPointName())
//                                .methods(methodInfos)
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("批量查询流程说明书完成，成功查询: {}, 总请求: {}", responses.size(), documentIds.size());
            return responses;

        } catch (Exception e) {
            log.error("批量查询流程说明书时发生错误，ID列表: {}", documentIds, e);
            return new ArrayList<>();
        }
    }

//    /**
//     * 将DocumentationMethod转换为MethodInfo
//     *
//     * @param method 方法实体
//     * @return 方法信息DTO
//     */
//    private ProcessDocumentationResponse.MethodInfo convertToMethodInfo(DocumentationMethod method) {
//        return ProcessDocumentationResponse.MethodInfo.builder()
//                .id(method.getId())
//                .methodId(method.getMethodId())
//                .methodName(method.getMethodName())
//                .description(method.getDescription())
//                .signature(method.getSignature())
//                .className(method.getClassName())
//                .callLevel(method.getCallLevel())
//                .methodType(method.getMethodType() != null ? method.getMethodType().name() : null)
//                .build();
//    }
}
