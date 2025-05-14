package com.puti.code.documentation.service;

import com.puti.code.ai.documentation.AIDocumentationService;
import com.puti.code.ai.documentation.DocumentationGenerationContext;
import com.puti.code.ai.documentation.dto.AIDocumentationResponse;
import com.puti.code.ai.vector.SpringVectorGenerator;
import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.entity.rdb.DocumentationMethod;
import com.puti.code.base.entity.rdb.DocumentationTask;
import com.puti.code.base.entity.vector.DocumentationVector;
import com.puti.code.base.enums.DocumentType;
import com.puti.code.base.model.MethodInfo;
import com.puti.code.base.model.SubgraphData;
import com.puti.code.documentation.config.DocumentationConfig;
import com.puti.code.documentation.dto.DocumentationGenerationDto;
import com.puti.code.documentation.repository.graph.SubgraphRepository;
import com.puti.code.documentation.repository.sql.DocumentationMethodRepository;
import com.puti.code.documentation.repository.sql.DocumentationRepository;
import com.puti.code.repository.milvus.DocumentationVectorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 渐进式文档生成服务
 * 负责管理分层次的文档生成流程
 *
 * @author diodehe
 */
@Slf4j
@Service
public class ProgressiveDocumentationService {

    /**
     * 批量插入向量的大小
     */
    private static final int BATCH_SIZE = 1;

    private final DocumentationConfig config;
    private final SubgraphRepository subgraphRepository;
    private final AIDocumentationService aiDocumentationService;
    private final DocumentationTaskService taskService;
    private final DocumentationRepository documentationRepository;
    private final DocumentationMethodRepository documentationMethodRepository;
    private final SpringVectorGenerator vectorGenerator;
    private final DocumentationVectorClient vectorClient;

    /**
     * 向量缓存列表，用于批量插入
     */
    private final List<DocumentationVector> vectorBatch = new ArrayList<>();

    /**
     * 用于保护向量批处理的锁
     */
    private final ReentrantLock batchLock = new ReentrantLock();

    @Autowired
    public ProgressiveDocumentationService(DocumentationConfig config,
                                           SubgraphRepository subgraphRepository,
                                           AIDocumentationService aiDocumentationService,
                                           DocumentationTaskService taskService,
                                           DocumentationRepository documentationRepository,
                                           DocumentationMethodRepository documentationMethodRepository,
                                           SpringVectorGenerator vectorGenerator,
                                           DocumentationVectorClient vectorClient) {
        this.config = config;
        this.subgraphRepository = subgraphRepository;
        this.aiDocumentationService = aiDocumentationService;
        this.taskService = taskService;
        this.documentationRepository = documentationRepository;
        this.documentationMethodRepository = documentationMethodRepository;
        this.vectorGenerator = vectorGenerator;
        this.vectorClient = vectorClient;
    }

    /**
     * 启动渐进式文档生成
     *
     * @param dto        请求体
     * @param entryPoint 入口点信息
     * @return 任务ID
     */
    public CompletableFuture<String> startProgressiveGeneration(DocumentationGenerationDto dto, MethodInfo entryPoint) {
        try {
            Integer level = dto.getLevel();
            String methodId = entryPoint.getMethodId();
            log.info("开始为入口点 {} 生成 {} 层级的渐进式文档", methodId, level);

            // 1. 创建生成任务
            DocumentationTask task = taskService.createTask(dto, entryPoint);

            // 2. 异步执行渐进式生成
            executeProgressiveGeneration(task);

            return CompletableFuture.completedFuture(task.getId());

        } catch (Exception e) {
            log.error("启动渐进式文档生成失败", e);
            return CompletableFuture.failedFuture(new RuntimeException("启动生成任务失败", e));
        }
    }

    /**
     * 执行渐进式生成流程
     */
    private void executeProgressiveGeneration(DocumentationTask task) {
        try {
            taskService.updateTaskStatus(task.getId(), DocumentationTask.TaskStatus.RUNNING);

            Documentation previousDoc = null;

            // 逐层生成文档
            for (int level = 1; level <= task.getTargetLevel(); level++) {
                log.info("开始生成第 {} 层文档，入口点: {}", level, task.getEntryPointId());

                // 更新任务进度
                int progress = (level - 1) * 100 / task.getTargetLevel();
                taskService.updateTaskProgress(task.getId(), level, progress);

                // 生成当前层级的文档
                DocumentationWithSubgraph docWithSubgraph = generateDocumentationForLevel(
                        task, level, previousDoc);

                if (docWithSubgraph == null || docWithSubgraph.documentation == null) {
                    throw new RuntimeException("第 " + level + " 层文档生成失败");
                }

                Documentation currentDoc = docWithSubgraph.documentation;
                SubgraphData subgraph = docWithSubgraph.subgraph;

                // 保存中间态文档
                saveIntermediateDocumentation(currentDoc, subgraph);

                previousDoc = currentDoc;

                log.info("第 {} 层文档生成完成，文档ID: {}", level, currentDoc.getId());
            }

//                // 标记最终版本
//                if (previousDoc != null) {
//                    markAsFinalVersion(previousDoc);
//
//                    // 异步归档中间态版本
//                    if (config.isAutoArchiveEnabled() && task.getTargetLevel() > 1) {
//                        archiveService.archiveIntermediateVersions(task.getEntryPointId(), previousDoc);
//                    }
//                }

            // 强制刷新剩余的向量
            forceFlushVectorBatch();

            // 完成任务
            taskService.completeTask(task.getId());
            log.info("渐进式文档生成完成，入口点: {}, 最终层级: {}",
                    task.getEntryPointId(), task.getTargetLevel());

        } catch (Exception e) {
            log.error("渐进式文档生成失败，任务ID: {}", task.getId(), e);
            // 即使失败也要刷新剩余的向量
            forceFlushVectorBatch();
            taskService.failTask(task.getId(), e.getMessage());
        }
    }

    /**
     * 内部类：文档和子图数据的包装
     */
    private static class DocumentationWithSubgraph {
        final Documentation documentation;
        final SubgraphData subgraph;

        DocumentationWithSubgraph(Documentation documentation, SubgraphData subgraph) {
            this.documentation = documentation;
            this.subgraph = subgraph;
        }
    }

    /**
     * 为指定层级生成文档
     */
    private DocumentationWithSubgraph generateDocumentationForLevel(DocumentationTask task, int level,
                                                                    Documentation previousDoc) {
        try {
            String entryPointId = task.getEntryPointId();
            // 1. 获取当前层级的子图数据
            int maxSteps = config.getMaxStepsForLevel(level);
            SubgraphData subgraph = subgraphRepository.getSubgraphByLevel(entryPointId, maxSteps);

            if (subgraph.isEmpty()) {
                log.warn("入口点 {} 的第 {} 层子图为空", entryPointId, level);
                return null;
            }

            // 2. 构建生成上下文
            DocumentationGenerationContext context = DocumentationGenerationContext.builder()
                    .entryPointId(entryPointId)
                    .level(level)
                    .subgraph(subgraph)
                    .previousDocumentation(previousDoc)
                    .projectId(task.getProjectId())
                    .branchName(task.getBranchName())
                    .maxLength(config.getMaxLengthForLevel(level))
                    .build();

            // 3. 调用AI生成文档
            AIDocumentationResponse aiResponse = aiDocumentationService.generateDocumentationForLevel(context);

            if (aiResponse == null || !aiResponse.isValid()) {
                log.error("AI生成的第 {} 层文档响应无效", level);
                return null;
            }

            // 4. 创建文档对象
            Documentation documentation = Documentation.builder()
                    .entryPointId(entryPointId)
                    .entryPointName(subgraph.getEntryPoint().getFullName())
                    .title(aiResponse.getCleanTitle()) // 使用AI生成的标题
                    .summary(aiResponse.getCleanSummary()) // 使用AI生成的摘要
                    .content(aiResponse.getCleanContent()) // 使用AI生成的内容
                    .level(level)
                    .status(Documentation.DocumentationStatus.COMPLETED)
                    .version(1)
                    .isFinalVersion(false) // 初始都是中间态
                    .parentDocumentationId(previousDoc != null ? previousDoc.getId() : null)
                    .projectId(task.getProjectId())
                    .branchName(task.getBranchName())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return new DocumentationWithSubgraph(documentation, subgraph);

        } catch (Exception e) {
            log.error("生成第 {} 层文档时发生错误", level, e);
            return null;
        }
    }

    /**
     * 保存中间态文档
     */
    private void saveIntermediateDocumentation(Documentation documentation, SubgraphData subgraph) {
        try {
            documentation.setIsFinalVersion(true);

            // 保存文档到数据库
            Documentation savedDoc = documentationRepository.save(documentation);
            if (savedDoc == null) {
                throw new RuntimeException("保存文档失败");
            }

            // 更新文档ID（如果是新创建的）
            if (documentation.getId() == null) {
                documentation.setId(savedDoc.getId());
            }

            // 同时保存方法信息
            saveDocumentationMethods(documentation, subgraph);

//            saveDocumentationVector(documentation, DocumentType.getByLevel(documentation.getLevel()));

            log.debug("保存文档成功，ID: {}, 层级: {}",
                    documentation.getId(), documentation.getLevel());

        } catch (Exception e) {
            log.error("保存中间态文档失败", e);
            throw new RuntimeException("保存文档失败", e);
        }
    }

    /**
     * 保存文档关联的方法信息
     */
    private void saveDocumentationMethods(Documentation documentation, SubgraphData subgraph) {
        try {
            if (documentation.getId() == null) {
                log.warn("文档ID为空，无法保存方法信息");
                return;
            }

            if (subgraph == null || CollectionUtils.isEmpty(subgraph.getMethods())) {
                log.warn("子图数据为空，无方法信息需要保存");
                return;
            }

            // 将MethodInfo转换为DocumentationMethod实体列表
            List<DocumentationMethod> documentationMethods = convertToDocumentationMethods(
                    documentation.getId(), subgraph.getMethods());

            if (!documentationMethods.isEmpty()) {
                // 批量保存方法信息
                List<DocumentationMethod> savedMethods = documentationMethodRepository.saveAll(documentationMethods);
                log.info("成功保存文档方法信息，文档ID: {}, 方法数量: {}",
                        documentation.getId(), savedMethods.size());
            }

        } catch (Exception e) {
            log.error("保存文档方法信息失败，文档ID: {}", documentation.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 将MethodInfo列表转换为DocumentationMethod实体列表
     */
    private List<DocumentationMethod> convertToDocumentationMethods(String documentationId, List<MethodInfo> methods) {
        List<DocumentationMethod> documentationMethods = new ArrayList<>();

        for (MethodInfo methodInfo : methods) {
            try {
                DocumentationMethod docMethod = DocumentationMethod.builder()
                        .documentationId(documentationId)
                        .methodId(methodInfo.getMethodId())
                        .methodName(methodInfo.getFullName())
                        .methodType(determineMethodType(methodInfo))
                        .callLevel(methodInfo.getLevel())
                        .description(methodInfo.getDescription())
                        .signature(methodInfo.getSignature())
                        .className(methodInfo.getClassName())
                        .createdAt(LocalDateTime.now())
                        .build();

                documentationMethods.add(docMethod);

            } catch (Exception e) {
                log.warn("转换方法信息失败，跳过该方法: {}", methodInfo.getMethodId(), e);
            }
        }

        return documentationMethods;
    }

    /**
     * 根据MethodInfo确定方法类型
     */
    private DocumentationMethod.MethodType determineMethodType(MethodInfo methodInfo) {
        // 根据方法信息判断方法类型
        if (Boolean.TRUE.equals(methodInfo.getIsEntryPoint())) {
            return DocumentationMethod.MethodType.ENTRY_POINT;
        }

        // 根据调用层级判断
        Integer level = methodInfo.getLevel();
        if (level != null) {
            if (level == 1) {
                return DocumentationMethod.MethodType.DIRECT_CALL;
            } else if (level > 1) {
                return DocumentationMethod.MethodType.INDIRECT_CALL;
            }
        }

        // 根据方法名判断是否为工具方法
        String methodName = methodInfo.getMethodName();
        if (methodName != null && (methodName.startsWith("get") || methodName.startsWith("set") ||
                methodName.startsWith("is") || methodName.contains("Util") ||
                methodName.contains("Helper"))) {
            return DocumentationMethod.MethodType.UTILITY_METHOD;
        }

        // 默认为间接调用
        return DocumentationMethod.MethodType.INDIRECT_CALL;
    }

    private void saveDocumentationVector(Documentation documentation, DocumentType documentType) {
        DocumentationVector vector = new DocumentationVector();
        String content = documentation.getTitle() + ":" + documentation.getSummary();
        float[] floats = vectorGenerator.generateVector(content);
        vector.setTextDense(floats);
        vector.setDocumentId(documentation.getId().toString());
        vector.setContent(content);
        vector.setDocumentType(documentType);
        vector.setProjectId(documentation.getProjectId());
        vector.setBranchName(documentation.getBranchName());
        // 添加到批量处理队列
        addVectorToBatch(vector);
    }

    /**
     * 将向量添加到批处理队列中
     * 当达到批量大小时自动执行批量插入
     */
    private void addVectorToBatch(DocumentationVector vector) {
        batchLock.lock();
        try {
            vectorBatch.add(vector);
            log.debug("向量已添加到批处理队列，当前队列大小: {}, 文档ID: {}",
                    vectorBatch.size(), vector.getDocumentId());

            // 当达到批量大小时执行批量插入
            if (vectorBatch.size() >= BATCH_SIZE) {
                flushVectorBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 执行批量插入向量数据库
     */
    private void flushVectorBatch() {
        if (vectorBatch.isEmpty()) {
            return;
        }

        List<DocumentationVector> batchToInsert = new ArrayList<>(vectorBatch);
        vectorBatch.clear();

        try {
//            vectorClient.batchUpsertDocumentationVectors(batchToInsert);
            log.info("批量插入文档向量成功，数量: {}", batchToInsert.size());
        } catch (Exception vectorException) {
            log.error("批量插入文档向量失败，数量: {}", batchToInsert.size(), vectorException);
            // 向量插入失败不影响主流程，只记录错误
        }
    }

    /**
     * 强制刷新剩余的向量到数据库
     * 通常在任务完成或服务关闭时调用
     */
    public void forceFlushVectorBatch() {
        batchLock.lock();
        try {
            if (!vectorBatch.isEmpty()) {
                log.info("强制刷新剩余向量，数量: {}", vectorBatch.size());
//                flushVectorBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }

}
