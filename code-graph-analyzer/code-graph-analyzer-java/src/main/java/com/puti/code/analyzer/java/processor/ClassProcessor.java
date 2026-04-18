package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.analyzer.java.support.ParseSupport;
import com.puti.code.base.enums.ParseType;
import com.puti.code.base.model.*;
import com.puti.code.base.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashSet;
import java.util.Set;

/**
 * 类处理器
 */
@Slf4j
public class ClassProcessor extends BaseProcessor<CtType<?>> {
    private final Set<String> processedClasses = new HashSet<>();
    private final DependencyInjectionResolutionSupport injectionSupport;

    public ClassProcessor(GraphContext graphContext, DependencyInjectionResolutionSupport injectionSupport) {
        super(graphContext);
        this.injectionSupport = injectionSupport;
    }

    @Override
    public void process(CtType<?> element) {
        try {
            String qualifiedName = element.getQualifiedName();

            String classId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(qualifiedName)
                    .isShadow(false).build());

            boolean matchDiscardRegx = config.getProjectDiscardRegxList().stream().anyMatch(qualifiedName::matches);
            if(matchDiscardRegx){
                getLogger().debug("Skipping discard type: {}", element);
                return;
            }

            // 避免重复处理
            if (processedClasses.contains(classId)) {
                return;
            }
            processedClasses.add(classId);

            // 确定类型
            String type = "class";
            if (element.isInterface()) {
                type = "interface";
            } else if (element.isEnum()) {
                type = "enum";
            } else if (element.isAnnotationType()) {
                type = "annotation";
            }

            // 确定可见性
            String visibility = "default";
            if (element.isPublic()) {
                visibility = "public";
            } else if (element.isPrivate()) {
                visibility = "private";
            } else if (element.isProtected()) {
                visibility = "protected";
            }

            // 创建类节点
            ClassNode classNode = ClassNode.builder()
                    .id(classId)
                    .nodeType(NodeType.CLASS)
                    .fullName(qualifiedName)
                    .name(element.getSimpleName())
                    .type(type)
                    .visibility(visibility)
                    .isLibrary(ParseType.LIBRARY.equals(parseType))
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

            // 处理类注释
            processClassComment(element, classId);

            // 处理类注解
            processClassAnnotations(element, classId);

            // 处理类依赖关系
            processClassDependencies(element, classId);

            // 处理类方法
            processMethods(element, classId);

            // 处理类构造函数
            processConstructors(element, classId);

            // 处理内部类
            processInnerClasses(element, classId);
        } catch (Exception e) {
            log.error("Failed to process class: {}", element.getQualifiedName(), e);
        }
    }

    /**
     * 处理类注释
     *
     * @param element 类元素
     * @param classId 类ID
     */
    private void processClassComment(CtType<?> element, String classId) {
        CtComment comment = element.getComments().stream()
                .filter(c -> c.getCommentType() == CtComment.CommentType.JAVADOC)
                .findFirst()
                .orElse(null);

        if (comment != null) {

            String commentId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(element.getQualifiedName() + "#comment")
                    .isShadow(false).build());

            CommentNode commentNode = CommentNode.builder()
                    .id(commentId)
                    .nodeType(NodeType.COMMENT)
                    .fullName(element.getQualifiedName() + "#comment")
                    .type("DOC")
                    .lineStart(comment.getPosition().getLine())
                    .lineEnd(comment.getPosition().getEndLine())
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .filePath(resolveFilePath(element))
                    .build();
            commentNode.setContent(comment.toString());

            processNode(commentNode);

            // 添加文档关系
            Edge edge = Edge.builder()
                    .srcId(classId)
                    .dstId(commentId)
                    .type(EdgeType.DOCUMENTED_BY)
                    .build();

            processEdge(edge);
        }
    }

    /**
     * 处理类注解
     *
     * @param element 类元素
     * @param classId 类ID
     */
    private void processClassAnnotations(CtType<?> element, String classId) {
        for (CtAnnotation<?> annotation : element.getAnnotations()) {
            CtTypeReference<?> annotationType = annotation.getAnnotationType();
            String annotationTypeName = annotationType.getQualifiedName();
            if (injectionSupport.isManagedFrameworkAnnotation(annotationTypeName)) {
                continue;
            }

            CtType<?> typeDeclaration = annotationType.getTypeDeclaration();
            boolean isShadow = typeDeclaration == null || typeDeclaration.isShadow();
            // 创建注解实例节点
            String annotationTypeId = IdGenerator.generate(IdGenerator.builder().
                    fullQualifiedName(annotationTypeName).isShadow(isShadow).build());
            String annotationId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(element.getQualifiedName() + "#" + annotationType.getSimpleName())
                    .isShadow(false).build());

            boolean isMarker = annotation.getValues().isEmpty();
            AnnotationNode annotationNode = AnnotationNode.builder()
                    .id(annotationId)
                    .nodeType(isMarker ? NodeType.MARKER_ANNOTATION : NodeType.ANNOTATION)
                    .fullName(element.getQualifiedName() + "#" + annotationType.getSimpleName())
                    .name(annotationType.getSimpleName())
                    .type(isMarker ? NodeType.MARKER_ANNOTATION.name() : NodeType.ANNOTATION.name())
                    .lineStart(annotation.getPosition().getLine())
                    .lineEnd(annotation.getPosition().getEndLine())
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .filePath(resolveFilePath(element))
                    .build();
            annotationNode.setContent(annotation.toString());

            processNode(annotationNode);

            // 添加类包含注解关系
            Edge containsEdge = Edge.builder()
                    .srcId(classId)
                    .dstId(annotationId)
                    .type(EdgeType.CONTAINS)
                    .build();

            processEdge(containsEdge);

            // 添加注解实例化关系
            Edge instanceOfEdge = Edge.builder()
                    .srcId(annotationId)
                    .dstId(annotationTypeId)
                    .type(EdgeType.INSTANCE_OF)
                    .lineNumber(annotation.getPosition().getLine())
                    .build();

            processEdge(instanceOfEdge);
        }
    }

    /**
     * 处理类依赖关系
     *
     * @param element 类元素
     * @param classId 类ID
     */
    private void processClassDependencies(CtType<?> element, String classId) {
        // 处理继承关系
        CtTypeReference<?> superclass = element.getSuperclass();
        if (superclass != null) {
            CtType<?> typeDeclaration = superclass.getTypeDeclaration();
            boolean shadow = typeDeclaration == null || typeDeclaration.isShadow();
            String superClassName = superclass.getQualifiedName();

            String superClassId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(superClassName)
                    .isShadow(shadow).build());

            Edge edge = Edge.builder()
                    .srcId(classId)
                    .dstId(superClassId)
                    .type(EdgeType.DEPENDS_ON)
                    .dependencyType(DependencyType.INHERITANCE)
                    .lineNumber(element.getPosition().getLine())
                    .build();

            processEdge(edge);
        }

        // 处理接口实现
        for (CtTypeReference<?> interfaceRef : element.getSuperInterfaces()) {
            if(ParseSupport.isIgnoreType(interfaceRef)){
                continue;
            }
            CtType<?> typeDeclaration = interfaceRef.getTypeDeclaration();
            boolean shadow = typeDeclaration == null || typeDeclaration.isShadow();
            String interfaceId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(interfaceRef.getQualifiedName())
                    .isShadow(shadow).build());

            Edge edge = Edge.builder()
                    .srcId(classId)
                    .dstId(interfaceId)
                    .type(EdgeType.DEPENDS_ON)
                    .dependencyType(DependencyType.IMPLEMENTATION)
                    .lineNumber(element.getPosition().getLine())
                    .build();

            processEdge(edge);
        }

        // 处理字段引用
        element.getFields().forEach(field -> {
            CtTypeReference<?> fieldType = field.getType();
            if (fieldType != null && !fieldType.isPrimitive()) {
                CtType<?> typeDeclaration = fieldType.getTypeDeclaration();
                boolean shadow = typeDeclaration == null || typeDeclaration.isShadow();

                String fieldTypeId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(fieldType.getQualifiedName())
                        .isShadow(shadow).build());

                Edge edge = Edge.builder()
                        .srcId(classId)
                        .dstId(fieldTypeId)
                        .type(EdgeType.DEPENDS_ON)
                        .dependencyType(DependencyType.ASSOCIATION)
                        .lineNumber(field.getPosition().getLine())
                        .build();

                processEdge(edge);
            }
        });
    }

    /**
     * 处理类方法
     *
     * @param element 类元素
     * @param classId 类ID
     */
    private void processMethods(CtType<?> element, String classId) {
        element.getMethods().forEach(method -> {
            if(ParseSupport.isSimpleGetter(method) || ParseSupport.isSimpleSetter(method)) {
                getLogger().debug("Skipping simple getter and setter: {}", method);
                return;
            }
            String methodSignature = ParseSupport.getExecutableSignature(element.getQualifiedName(), method);
            String methodId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(methodSignature)
                    .isShadow(false).build());

            // 添加类包含方法关系
            Edge edge = Edge.builder()
                    .srcId(classId)
                    .dstId(methodId)
                    .type(EdgeType.CONTAINS)
                    .build();

            processEdge(edge);
        });
    }

    /**
     * 处理类构造函数
     *
     * @param element 类元素
     * @param classId 类ID
     */
    private void processConstructors(CtType<?> element, String classId) {
        if (element instanceof CtClass<?> ctClass) {
            ctClass.getConstructors().forEach(constructor -> {
                // 跳过隐式构造函数（编译器自动生成的默认构造函数）
                if (constructor.isImplicit()) {
                    return;
                }

                String constructorId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(ParseSupport.getExecutableSignature(element.getQualifiedName(), constructor))
                        .isShadow(false).build());

                // 添加类包含构造函数关系
                Edge edge = Edge.builder()
                        .srcId(classId)
                        .dstId(constructorId)
                        .type(EdgeType.CONTAINS)
                        .build();

                processEdge(edge);
            });
        }
    }

    /**
     * 处理内部类
     *
     * @param element 类元素
     * @param classId 类ID
     */
    private void processInnerClasses(CtType<?> element, String classId) {
        element.getNestedTypes().forEach(nestedType -> {

            String nestedTypeId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(nestedType.getQualifiedName())
                    .isShadow(false).build());

            // 添加类包含内部类关系
            Edge edge = Edge.builder()
                    .srcId(classId)
                    .dstId(nestedTypeId)
                    .type(EdgeType.CONTAINS)
                    .build();

            processEdge(edge);

            // 递归处理内部类
            process(nestedType);
        });
    }

    @Override
    protected Logger getLogger(){
        return log;
    }
}
