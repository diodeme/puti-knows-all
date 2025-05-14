package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.analyzer.java.support.ParseSupport;
import com.puti.code.base.model.AnnotationNode;
import com.puti.code.base.model.DependencyType;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.model.FieldNode;
import com.puti.code.base.model.FunctionNode;
import com.puti.code.base.model.NodeType;
import com.puti.code.base.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 依赖注入事实提取与增强入口。
 */
@Slf4j
public class DependencyInjectionProcessor extends BaseProcessor<CtType<?>> {

    private final Set<String> processedTypes = new HashSet<>();
    private final List<PendingFieldBinding> pendingFieldBindings = new ArrayList<>();
    private final DependencyInjectionEnhancer dependencyInjectionEnhancer = new DependencyInjectionEnhancer();
    private final DependencyInjectionResolutionSupport injectionSupport;

    public DependencyInjectionProcessor(GraphContext graphContext,
                                        DependencyInjectionResolutionSupport injectionSupport) {
        super(graphContext);
        this.injectionSupport = injectionSupport;
    }

    @Override
    public void process(CtType<?> element) {
        try {
            String typeId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(element.getQualifiedName())
                    .isShadow(false)
                    .build());
            if (!processedTypes.add(typeId)) {
                return;
            }

            processProviderAnnotations(element, typeId);
            processFieldDependencies(element, typeId);
            processMethodDependencies(element, typeId);
        } catch (Exception e) {
            log.error("Failed to process dependency injection for {}", element.getQualifiedName(), e);
        }
    }

    private void processProviderAnnotations(CtType<?> element, String typeId) {
        for (CtAnnotation<?> annotation : element.getAnnotations()) {
            String annotationName = annotation.getAnnotationType().getQualifiedName();
            if (!injectionSupport.isTypeProviderAnnotation(annotationName)) {
                continue;
            }

            Map<String, Object> annotationValues = getAnnotationValues(annotation, element);
            injectionSupport.registerTypeProvider(element, annotation, annotationValues);

            String annotationId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(element.getQualifiedName() + "#" + annotationName)
                    .isShadow(false)
                    .build());
            AnnotationNode annotationNode = AnnotationNode.builder()
                    .id(annotationId)
                    .nodeType(NodeType.ANNOTATION)
                    .fullName(element.getQualifiedName() + "#" + annotationName)
                    .name(annotation.getAnnotationType().getSimpleName())
                    .type(NodeType.ANNOTATION.name())
                    .lineStart(annotation.getPosition().getLine())
                    .lineEnd(annotation.getPosition().getEndLine())
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .build();
            annotationNode.setContent(buildAnnotationContent(annotation, annotationValues));
            processNode(annotationNode);
            processEdge(Edge.builder()
                    .srcId(typeId)
                    .dstId(annotationId)
                    .type(EdgeType.CONTAINS)
                    .build());
            processAnnotationDependencies(annotation, annotationValues, annotationId);
        }
    }

    private void processFieldDependencies(CtType<?> element, String typeId) {
        for (CtField<?> field : element.getFields()) {
            ResolvedInjectionPoint resolvedInjectionPoint =
                    injectionSupport.resolveFieldInjection(element, field.getSimpleName()).orElse(null);
            if (resolvedInjectionPoint == null) {
                continue;
            }
            InjectionPoint injectionPoint = resolvedInjectionPoint.injectionPoint();

            String fieldId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(element.getQualifiedName() + "#" + field.getSimpleName())
                    .isShadow(false)
                    .build());

            FieldNode fieldNode = FieldNode.builder()
                    .id(fieldId)
                    .nodeType(NodeType.FIELD)
                    .fullName(element.getQualifiedName() + "#" + field.getSimpleName())
                    .name(field.getSimpleName())
                    .type(field.getType().getSimpleName())
                    .visibility(determineVisibility(field))
                    .isStatic(field.isStatic())
                    .lineStart(field.getPosition().getLine())
                    .lineEnd(field.getPosition().getEndLine())
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .build();
            processNode(fieldNode);
            processEdge(Edge.builder()
                    .srcId(typeId)
                    .dstId(fieldId)
                    .type(EdgeType.CONTAINS)
                    .build());

            CtTypeReference<?> fieldType = field.getType();
            if (fieldType != null && !fieldType.isPrimitive()) {
                CtType<?> typeDeclaration = fieldType.getTypeDeclaration();
                boolean isShadow = typeDeclaration == null || typeDeclaration.isShadow();
                String fieldTypeName = fieldType.getQualifiedName();
                String fieldTypeId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(fieldTypeName)
                        .isShadow(isShadow)
                        .build());
                processEdge(Edge.builder()
                        .srcId(fieldId)
                        .dstId(fieldTypeId)
                        .type(EdgeType.DEPENDS_ON)
                        .dependencyType(DependencyType.INJECTION)
                        .lineNumber(field.getPosition().getLine())
                        .build());

                if (isStrategyMap(fieldType)) {
                    processStrategyMapInjection(field, fieldId, fieldType);
                }
            }

            pendingFieldBindings.add(new PendingFieldBinding(
                    element.getQualifiedName(),
                    field.getSimpleName(),
                    fieldId));

            processFieldAnnotations(element, field, fieldId);
        }
    }

    private void processFieldAnnotations(CtType<?> element, CtField<?> field, String fieldId) {
        for (CtAnnotation<?> annotation : field.getAnnotations()) {
            String annotationName = annotation.getAnnotationType().getQualifiedName();
            if (!injectionSupport.isInjectionOrQualifierAnnotation(annotationName)) {
                continue;
            }

            String annotationId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(element.getQualifiedName() + "#" + field.getSimpleName() + "#" + annotationName)
                    .isShadow(false)
                    .build());
            AnnotationNode annotationNode = AnnotationNode.builder()
                    .id(annotationId)
                    .nodeType(NodeType.ANNOTATION)
                    .fullName(element.getQualifiedName() + "#" + field.getSimpleName() + "#" + annotationName)
                    .name(annotation.getAnnotationType().getSimpleName())
                    .type(NodeType.ANNOTATION.name())
                    .lineStart(annotation.getPosition().getLine())
                    .lineEnd(annotation.getPosition().getEndLine())
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .build();
            annotationNode.setContent(annotation.toString());
            processNode(annotationNode);
            processEdge(Edge.builder()
                    .srcId(fieldId)
                    .dstId(annotationId)
                    .type(EdgeType.CONTAINS)
                    .build());
        }
    }

    private void processStrategyMapInjection(CtField<?> field, String fieldId, CtTypeReference<?> fieldType) {
        try {
            List<CtTypeReference<?>> typeArguments = fieldType.getActualTypeArguments();
            if (typeArguments.size() != 2) {
                return;
            }
            CtTypeReference<?> valueType = typeArguments.get(1);
            if (valueType.getTypeDeclaration() == null || !valueType.getTypeDeclaration().isInterface()) {
                return;
            }
            for (CtType<?> implementation : getFactory().Type().getAll()) {
                if (!implementation.isSubtypeOf(valueType)) {
                    continue;
                }
                String implementationId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(implementation.getQualifiedName())
                        .isShadow(false)
                        .build());
                processEdge(Edge.builder()
                        .srcId(fieldId)
                        .dstId(implementationId)
                        .type(EdgeType.INSTANCE_OF)
                        .dependencyType(DependencyType.INJECTION)
                        .lineNumber(field.getPosition().getLine())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to process strategy map injection for field {}", field.getSimpleName(), e);
        }
    }

    private void processMethodDependencies(CtType<?> element, String typeId) {
        for (CtMethod<?> method : element.getMethods()) {
            for (CtAnnotation<?> annotation : method.getAnnotations()) {
                String annotationName = annotation.getAnnotationType().getQualifiedName();
                if (!injectionSupport.isMethodProviderAnnotation(annotationName)) {
                    continue;
                }

                injectionSupport.registerMethodProvider(method, annotation);
                String methodSignature = ParseSupport.getExecutableSignature(element.getQualifiedName(), method);
                String methodId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(methodSignature)
                        .isShadow(false)
                        .build());
                String annotationId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(methodSignature + "#" + annotationName)
                        .isShadow(false)
                        .build());

                FunctionNode methodNode = FunctionNode.builder()
                        .id(methodId)
                        .nodeType(NodeType.FUNCTION)
                        .fullName(methodSignature)
                        .name(method.getSimpleName())
                        .visibility(determineVisibility(method))
                        .isStatic(method.isStatic())
                        .isConstructor(false)
                        .lineStart(method.getPosition().getLine())
                        .lineEnd(method.getPosition().getEndLine())
                        .branchName(config.getBranch())
                        .commitStatus("COMMITTED")
                        .lastUpdated(now())
                        .repoId(config.getProjectId())
                        .build();
                processNode(methodNode);

                AnnotationNode annotationNode = AnnotationNode.builder()
                        .id(annotationId)
                        .nodeType(NodeType.ANNOTATION)
                        .fullName(methodSignature + "#" + annotationName)
                        .name(annotation.getAnnotationType().getSimpleName())
                        .type(NodeType.ANNOTATION.name())
                        .lineStart(annotation.getPosition().getLine())
                        .lineEnd(annotation.getPosition().getEndLine())
                        .branchName(config.getBranch())
                        .commitStatus("COMMITTED")
                        .lastUpdated(now())
                        .repoId(config.getProjectId())
                        .build();
                annotationNode.setContent(buildAnnotationContent(annotation, getAnnotationValues(annotation, element)));
                processNode(annotationNode);

                processEdge(Edge.builder()
                        .srcId(methodId)
                        .dstId(typeId)
                        .type(EdgeType.DEPENDS_ON)
                        .dependencyType(DependencyType.BEAN_DEFINITION)
                        .lineNumber(method.getPosition().getLine())
                        .build());
                processEdge(Edge.builder()
                        .srcId(methodId)
                        .dstId(annotationId)
                        .type(EdgeType.CONTAINS)
                        .build());
            }
        }
    }

    private Map<String, Object> getAnnotationValues(CtAnnotation<?> annotation, CtType<?> declaringType) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, CtExpression> entry : annotation.getValues().entrySet()) {
            values.put(entry.getKey(), resolveExpressionValue(entry.getValue(), declaringType));
        }
        return values;
    }

    private Object resolveExpressionValue(CtExpression<?> value, CtType<?> declaringType) {
        if (value instanceof CtLiteral<?> literal) {
            return literal.getValue();
        }
        if (value instanceof CtFieldAccess<?> fieldAccess) {
            return extractFieldAccessValue(fieldAccess);
        }
        if (value instanceof CtVariableAccess<?> variableAccess) {
            return extractVariableAccessValue(variableAccess, declaringType);
        }
        return value != null ? value.toString() : null;
    }

    private String extractFieldAccessValue(CtFieldAccess<?> fieldAccess) {
        try {
            CtTypeReference<?> declaringType = fieldAccess.getVariable().getDeclaringType();
            if (declaringType == null) {
                return fieldAccess.getVariable().getSimpleName();
            }
            CtType<?> type = getType(declaringType.getQualifiedName());
            if (type != null) {
                CtField<?> field = type.getField(fieldAccess.getVariable().getSimpleName());
                if (field != null && field.getDefaultExpression() instanceof CtLiteral<?> literal) {
                    return String.valueOf(literal.getValue());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve field access value", e);
        }
        return fieldAccess.getVariable().getSimpleName();
    }

    private String extractVariableAccessValue(CtVariableAccess<?> variableAccess, CtType<?> declaringType) {
        try {
            if (declaringType != null) {
                CtField<?> field = declaringType.getField(variableAccess.getVariable().getSimpleName());
                if (field != null && field.getDefaultExpression() instanceof CtLiteral<?> literal) {
                    return String.valueOf(literal.getValue());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve variable access value", e);
        }
        return variableAccess.getVariable().getSimpleName();
    }

    private String buildAnnotationContent(CtAnnotation<?> annotation, Map<String, Object> values) {
        StringBuilder content = new StringBuilder();
        content.append("@").append(annotation.getAnnotationType().getSimpleName());
        if (!values.isEmpty()) {
            List<String> pairs = new ArrayList<>();
            values.forEach((key, value) -> {
                if (value instanceof String stringValue) {
                    pairs.add(key + "=\"" + stringValue + "\"");
                } else {
                    pairs.add(key + "=" + value);
                }
            });
            content.append("(").append(String.join(", ", pairs)).append(")");
        }
        return content.toString();
    }

    private void processAnnotationDependencies(CtAnnotation<?> annotation, Map<String, Object> values, String annotationId) {
        for (Object value : values.values()) {
            if (!(value instanceof String stringValue) || !stringValue.contains(".")) {
                continue;
            }
            String typeId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(stringValue)
                    .isShadow(false)
                    .build());
            processEdge(Edge.builder()
                    .srcId(annotationId)
                    .dstId(typeId)
                    .type(EdgeType.DEPENDS_ON)
                    .dependencyType(DependencyType.USAGE)
                    .lineNumber(annotation.getPosition().getLine())
                    .build());
        }
    }

    private boolean isStrategyMap(CtTypeReference<?> fieldType) {
        return fieldType != null
                && "java.util.Map".equals(fieldType.getQualifiedName())
                && fieldType.getActualTypeArguments().size() == 2;
    }

    private String determineVisibility(CtMethod<?> method) {
        if (method.isPublic()) {
            return "public";
        }
        if (method.isPrivate()) {
            return "private";
        }
        if (method.isProtected()) {
            return "protected";
        }
        return "default";
    }

    private String determineVisibility(CtField<?> field) {
        if (field.isPublic()) {
            return "public";
        }
        if (field.isPrivate()) {
            return "private";
        }
        if (field.isProtected()) {
            return "protected";
        }
        return "default";
    }

    private CtType<?> getType(String typeName) {
        try {
            return getFactory().Type().get(typeName);
        } catch (Exception e) {
            log.debug("Failed to resolve type {}", typeName, e);
            return null;
        }
    }

    private void emitPendingFieldBindings() {
        for (PendingFieldBinding pendingBinding : pendingFieldBindings) {
            CtType<?> ownerType = getType(pendingBinding.ownerTypeName());
            if (ownerType == null) {
                continue;
            }
            CtField<?> field = ownerType.getField(pendingBinding.fieldName());
            if (field == null) {
                continue;
            }
            ResolvedInjectionPoint resolvedInjectionPoint =
                    injectionSupport.resolveFieldInjection(ownerType, pendingBinding.fieldName()).orElse(null);
            if (resolvedInjectionPoint == null) {
                continue;
            }
            DependencyInjectionResolution resolution = resolvedInjectionPoint.resolution();
            if (!resolution.isCandidateMatched() || resolution.getResolvedTypeName() == null) {
                continue;
            }

            CtType<?> resolvedType = getType(resolution.getResolvedTypeName());
            boolean isShadow = resolvedType == null || resolvedType.isShadow();
            String implementationId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(resolution.getResolvedTypeName())
                    .isShadow(isShadow)
                    .build());
            int lineNumber = field.getPosition() != null && field.getPosition().isValidPosition()
                    ? field.getPosition().getLine()
                    : -1;
            DependencyInjectionFact fact = DependencyInjectionFact.fromBinding(
                    resolvedInjectionPoint.injectionPoint(),
                    resolution,
                    lineNumber);
            dependencyInjectionEnhancer.createBindingEdge(fact, pendingBinding.fieldId(), implementationId)
                    .ifPresent(this::processEdge);
        }
        pendingFieldBindings.clear();
    }

    @Override
    public void processingDone() {
        emitPendingFieldBindings();
        super.processingDone();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private record PendingFieldBinding(String ownerTypeName, String fieldName, String fieldId) {
    }
}
