package com.puti.code.documentation.service;

import com.puti.code.documentation.config.DocumentationConfig;
import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.entity.rdb.DocumentationArchive;
import com.puti.code.base.entity.rdb.DocumentationMethod;
import com.puti.code.base.entity.rdb.DocumentationMethodArchive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 说明书归档服务
 * 负责管理中间态文档的归档和清理
 *
 * @author diodehe
 */
@Slf4j
@Service
public class DocumentationArchiveService {

    private final DocumentationConfig config;

    @Autowired
    public DocumentationArchiveService(DocumentationConfig config) {
        this.config = config;
    }
    
    /**
     * 当最终版本完成时，归档所有中间态版本
     * 
     * @param entryPointId 入口点ID
     * @param finalDocumentation 最终版本文档
     */
    public CompletableFuture<Void> archiveIntermediateVersions(String entryPointId, Documentation finalDocumentation) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("开始归档入口点 {} 的中间态版本", entryPointId);
                
                // 1. 查找所有中间态版本
                List<Documentation> intermediateVersions = findIntermediateVersions(entryPointId);
                
                if (intermediateVersions.isEmpty()) {
                    log.info("入口点 {} 没有需要归档的中间态版本", entryPointId);
                    return;
                }
                
                // 2. 归档每个中间态版本
                for (Documentation intermediate : intermediateVersions) {
                    archiveSingleDocumentation(intermediate, finalDocumentation.getId(), 
                            DocumentationArchive.ArchiveReason.FINAL_VERSION_COMPLETED);
                }
                
                // 3. 删除原始中间态记录
                deleteIntermediateVersions(intermediateVersions);
                
                log.info("成功归档入口点 {} 的 {} 个中间态版本", entryPointId, intermediateVersions.size());
                
            } catch (Exception e) {
                log.error("归档入口点 {} 的中间态版本时发生错误", entryPointId, e);
                throw new RuntimeException("归档失败", e);
            }
        });
    }
    
    /**
     * 归档单个文档及其关联的方法信息
     */
    private void archiveSingleDocumentation(Documentation documentation, String finalDocumentationId,
                                           DocumentationArchive.ArchiveReason reason) {
        
        // 1. 创建文档归档记录
        DocumentationArchive archive = DocumentationArchive.builder()
                .originalDocumentationId(documentation.getId())
                .entryPointId(documentation.getEntryPointId())
                .entryPointName(documentation.getEntryPointName())
                .title(documentation.getTitle())
                .content(documentation.getContent())
                .level(documentation.getLevel())
                .version(documentation.getVersion())
                .projectId(documentation.getProjectId())
                .branchName(documentation.getBranchName())
                .originalCreatedAt(documentation.getCreatedAt())
                .originalUpdatedAt(documentation.getUpdatedAt())
                .archivedAt(LocalDateTime.now())
                .archiveReason(reason)
                .finalDocumentationId(finalDocumentationId)
                .build();
        
        // 2. 保存文档归档记录
        saveDocumentationArchive(archive);
        
        // 3. 归档关联的方法信息
        List<DocumentationMethod> methods = findMethodsByDocumentationId(documentation.getId());
        for (DocumentationMethod method : methods) {
            archiveDocumentationMethod(method, archive.getId());
        }
        
        log.debug("已归档文档 ID: {}, 层级: {}, 版本: {}", 
                documentation.getId(), documentation.getLevel(), documentation.getVersion());
    }
    
    /**
     * 归档方法信息
     */
    private void archiveDocumentationMethod(DocumentationMethod method, String archivedDocumentationId) {
        DocumentationMethodArchive methodArchive = DocumentationMethodArchive.builder()
                .originalMethodId(method.getId())
                .archivedDocumentationId(archivedDocumentationId)
                .methodId(method.getMethodId())
                .methodName(method.getMethodName())
                .methodType(method.getMethodType())
                .callLevel(method.getCallLevel())
                .description(method.getDescription())
                .signature(method.getSignature())
                .className(method.getClassName())
                .originalCreatedAt(method.getCreatedAt())
                .archivedAt(LocalDateTime.now())
                .build();
        
        saveDocumentationMethodArchive(methodArchive);
    }
    
    /**
     * 定期清理过期的归档数据
     */
    @Async("codeDocumentTaskExecutor")
    public CompletableFuture<Void> cleanupExpiredArchives() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(config.getArchiveDelayDays() * 2);

            // 删除过期的归档记录
            int deletedArchives = deleteExpiredArchives(cutoffDate);
            int deletedMethodArchives = deleteExpiredMethodArchives(cutoffDate);

            log.info("清理完成，删除了 {} 个文档归档记录和 {} 个方法归档记录",
                    deletedArchives, deletedMethodArchives);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("清理过期归档数据时发生错误", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 手动归档指定文档
     */
    public void manualArchive(String documentationId, String reason) {
        Documentation documentation = findDocumentationById(documentationId);
        if (documentation != null) {
            archiveSingleDocumentation(documentation, null, 
                    DocumentationArchive.ArchiveReason.MANUAL_ARCHIVE);
            deleteDocumentation(documentation);
            log.info("手动归档文档 ID: {}, 原因: {}", documentationId, reason);
        }
    }
    
    /**
     * 检查是否需要清理中间版本
     */
    public void checkAndCleanupIntermediateVersions(String entryPointId) {
        List<Documentation> intermediateVersions = findIntermediateVersions(entryPointId);
        
        if (intermediateVersions.size() > config.getMaxIntermediateVersions()) {
            // 保留最新的几个版本，归档其余的
            List<Documentation> toArchive = intermediateVersions.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .skip(config.getMaxIntermediateVersions())
                    .toList();
            
            for (Documentation doc : toArchive) {
                archiveSingleDocumentation(doc, null, 
                        DocumentationArchive.ArchiveReason.VERSION_SUPERSEDED);
                deleteDocumentation(doc);
            }
            
            log.info("清理入口点 {} 的过多中间版本，归档了 {} 个版本", entryPointId, toArchive.size());
        }
    }
    
    // 以下是需要实现的数据访问方法（具体实现取决于存储方案）
    
    private List<Documentation> findIntermediateVersions(String entryPointId) {
        // TODO: 实现查找中间态版本的逻辑
        // 查询条件：entryPointId = ? AND isFinalVersion = false
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private Documentation findDocumentationById(String id) {
        // TODO: 实现根据ID查找文档的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private List<DocumentationMethod> findMethodsByDocumentationId(String documentationId) {
        // TODO: 实现查找文档关联方法的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private void saveDocumentationArchive(DocumentationArchive archive) {
        // TODO: 实现保存文档归档记录的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private void saveDocumentationMethodArchive(DocumentationMethodArchive methodArchive) {
        // TODO: 实现保存方法归档记录的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private void deleteIntermediateVersions(List<Documentation> versions) {
        // TODO: 实现删除中间态版本的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private void deleteDocumentation(Documentation documentation) {
        // TODO: 实现删除文档的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private int deleteExpiredArchives(LocalDateTime cutoffDate) {
        // TODO: 实现删除过期归档记录的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
    
    private int deleteExpiredMethodArchives(LocalDateTime cutoffDate) {
        // TODO: 实现删除过期方法归档记录的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }
}
