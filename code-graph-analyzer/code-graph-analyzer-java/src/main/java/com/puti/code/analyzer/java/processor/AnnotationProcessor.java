package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.base.model.ClassNode;
import com.puti.code.base.model.NodeType;
import com.puti.code.base.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import spoon.reflect.declaration.CtAnnotationType;

import java.util.HashSet;
import java.util.Set;

/**
 * 注解类型处理器
 */
@Slf4j
public class AnnotationProcessor extends BaseProcessor<CtAnnotationType<?>> {
    private final Set<String> processedAnnotations = new HashSet<>();

    public AnnotationProcessor(GraphContext graphContext) {
        super(graphContext);
    }

    @Override
    public void process(CtAnnotationType<?> element) {
        try {
            String qualifiedName = element.getQualifiedName();

            String annotationId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(qualifiedName)
                    .isShadow(false).build());

            // 避免重复处理
            if (processedAnnotations.contains(annotationId)) {
                return;
            }
            processedAnnotations.add(annotationId);

            // 确定可见性
            String visibility = "default";
            if (element.isPublic()) {
                visibility = "public";
            } else if (element.isPrivate()) {
                visibility = "private";
            } else if (element.isProtected()) {
                visibility = "protected";
            }

            // 创建注解类节点
            ClassNode classNode = ClassNode.builder()
                    .id(annotationId)
                    .nodeType(NodeType.CLASS)
                    .fullName(qualifiedName)
                    .name(element.getSimpleName())
                    .type("annotation")
                    .visibility(visibility)
                    .lineStart(element.getPosition().getLine())
                    .lineEnd(element.getPosition().getEndLine())
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .isExternal(false)
                    .filePath(resolveFilePath(element))
                    .build();
            classNode.setContent(element.toString());

            processNode(classNode);
        } catch (Exception e) {
            log.error("Failed to process annotation: {}", element.getQualifiedName(), e);
        }
    }

    @Override
    protected Logger getLogger(){
        return log;
    }
}
