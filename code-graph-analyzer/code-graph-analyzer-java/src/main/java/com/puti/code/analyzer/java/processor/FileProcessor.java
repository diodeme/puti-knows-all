package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.base.model.*;
import com.puti.code.base.enums.ParseType;
import com.puti.code.base.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import spoon.reflect.declaration.CtCompilationUnit;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 文件处理器
 */
@Slf4j
public class FileProcessor extends BaseProcessor<CtCompilationUnit> {
    
    public FileProcessor(GraphContext graphContext) {
        super(graphContext);
    }
    
    @Override
    public void process(CtCompilationUnit element) {
        try {
            File file = element.getFile();
            if (file == null || !file.exists()) {
                return;
            }
            
            String absolutePath = file.getAbsolutePath();
            String relativePath = config.getRelativePath(absolutePath);
            String fileId = IdGenerator.generate(IdGenerator.builder().
                    fullQualifiedName(relativePath).isShadow(false).build());
            
            FileNode fileNode = FileNode.builder()
                    .id(fileId)
                    .nodeType(NodeType.FILE)
                    .fullName(relativePath)
                    .filePath(relativePath)
                    .name(file.getName())
                    .extension(FilenameUtils.getExtension(file.getName()))
                    .isLibrary(ParseType.LIBRARY.equals(parseType))
                    .lastModified(LocalDateTime.ofInstant(new Date(file.lastModified()).toInstant(), ZoneId.systemDefault()))
                    .language("java")
                    .branchName(config.getBranch()) // 可以通过Git命令获取当前分支
                    .commitStatus("COMMITTED") // 可以通过Git命令获取提交状态
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .build();
            
            processNode(fileNode);

            // 处理文件与类的关系
            element.getDeclaredTypes().forEach(type -> {
                String typeId = IdGenerator.generate(IdGenerator.builder().
                        fullQualifiedName(type.getQualifiedName()).isShadow(false).build());
                Edge edge = Edge.builder()
                        .srcId(fileId)
                        .dstId(typeId)
                        .type(EdgeType.CONTAINS)
                        .build();
                
                try {
                    processEdge(edge);
                } catch (Exception e) {
                    log.error("Failed to insert edge: {} -> {}", fileId, typeId, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to process file: {}", element.getFile().getAbsolutePath(), e);
        }
    }

    @Override
    protected Logger getLogger(){
        return log;
    }
}
