package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.analyzer.java.rule.SpoonRuleContextBuilder;
import com.puti.code.analyzer.java.support.ParseSupport;
import com.puti.code.base.enums.ParseType;
import com.puti.code.base.enums.RuleEngineType;
import com.puti.code.base.model.AnnotationNode;
import com.puti.code.base.model.CommentNode;
import com.puti.code.base.model.DependencyType;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.model.FunctionNode;
import com.puti.code.base.model.NodeType;
import com.puti.code.base.util.IdGenerator;
import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.engine.RuleEngine;
import com.puti.code.rule.factory.RuleEngineFactory;
import org.apache.commons.lang.StringUtils;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 可执行元素处理器抽象类，用于处理方法和构造函数的共同逻辑。
 */
public abstract class ExecutableProcessor<T extends CtExecutable<?>> extends BaseProcessor<T> {

    protected final Set<String> processedExecutables = new HashSet<>();
    private final DependencyInjectionResolutionSupport injectionSupport;
    private final DependencyInjectionEnhancer dependencyInjectionEnhancer = new DependencyInjectionEnhancer();

    protected ExecutableProcessor(GraphContext graphContext, DependencyInjectionResolutionSupport injectionSupport) {
        super(graphContext);
        this.injectionSupport = injectionSupport;
    }

    public void processExecutable(T element) {
        try {
            if (Objects.nonNull(element.getReference().getDeclaringType())
                    && Objects.nonNull(element.getReference().getDeclaringType().getTopLevelType().getPackage())) {
                Optional<CtTypeReference<?>> ctTypeReferenceOpt = element.getReference().getReferencedTypes().stream()
                        .filter(reference -> reference.getPackage() != null)
                        .findFirst();
                if (ctTypeReferenceOpt.isEmpty()) {
                    return;
                }
                String qualifiedName = ctTypeReferenceOpt.get().getPackage().getQualifiedName();
                if (StringUtils.isNotBlank(qualifiedName)
                        && StringUtils.isNotBlank(config.getTargetPackage())
                        && !qualifiedName.startsWith(config.getTargetPackage())) {
                    return;
                }
            }
            if (element.isImplicit()) {
                getLogger().debug("Skipping implicit executable: {}", element);
                return;
            }
            if (ParseSupport.isSimpleGetter(element) || ParseSupport.isSimpleSetter(element)) {
                getLogger().debug("Skipping simple getter/setter: {}", element);
                return;
            }

            CtType<?> declaringType = getDeclaringType(element);
            if (declaringType == null) {
                return;
            }

            String fullName = ParseSupport.getExecutableSignature(declaringType.getQualifiedName(), element);
            if (config.getProjectDiscardRegxList().stream().anyMatch(fullName::matches)) {
                getLogger().debug("Skipping discard executable: {}", fullName);
                return;
            }

            String executableId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(fullName)
                    .isShadow(false)
                    .build());
            if (!processedExecutables.add(executableId)) {
                return;
            }

            String executableName = getExecutableName(element, declaringType);
            String visibility = determineVisibility(element);
            FunctionNode functionNode = createFunctionNode(element, executableId, executableName, fullName, visibility);
            processNode(functionNode);

            processExecutableComment(element, executableId);
            processExecutableAnnotations(element, executableId);
            processExecutableParameterDependencies(element, executableId);
            processOverride(element, executableId);
            processExecutableCallDependencies(element, executableId);
        } catch (Exception e) {
            getLogger().error("Failed to process executable: {}", element, e);
        }
    }

    protected void processExecutableComment(T element, String executableId) {
        CtComment comment = element.getComments().stream()
                .filter(current -> current.getCommentType() == CtComment.CommentType.JAVADOC)
                .findFirst()
                .orElse(null);
        if (comment == null) {
            return;
        }

        CtType<?> declaringType = getDeclaringType(element);
        String executableSignature = ParseSupport.getExecutableSignature(declaringType.getQualifiedName(), element);
        String commentId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName(executableSignature + "#comment")
                .isShadow(false)
                .build());

        CommentNode commentNode = CommentNode.builder()
                .id(commentId)
                .nodeType(NodeType.COMMENT)
                .fullName(declaringType.getQualifiedName() + "#" + executableSignature + "#comment")
                .type("DOC")
                .lineStart(comment.getPosition().isValidPosition() ? comment.getPosition().getLine() : -1)
                .lineEnd(comment.getPosition().isValidPosition() ? comment.getPosition().getEndLine() : -1)
                .branchName(config.getBranch())
                .commitStatus("COMMITTED")
                .lastUpdated(now())
                .repoId(config.getProjectId())
                .build();
        commentNode.setContent(comment.toString());
        processNode(commentNode);
        processEdge(Edge.builder()
                .srcId(executableId)
                .dstId(commentId)
                .type(EdgeType.DOCUMENTED_BY)
                .build());
    }

    protected void processExecutableAnnotations(T element, String executableId) {
        CtType<?> declaringType = getDeclaringType(element);
        String executableSignature = ParseSupport.getExecutableSignature(declaringType.getQualifiedName(), element);

        for (CtAnnotation<?> annotation : element.getAnnotations()) {
            CtTypeReference<?> annotationType = annotation.getAnnotationType();
            String annotationTypeName = annotationType.getQualifiedName();
            if (injectionSupport.isManagedFrameworkAnnotation(annotationTypeName)) {
                continue;
            }

            CtType<?> typeDeclaration = annotationType.getTypeDeclaration();
            boolean isShadow = typeDeclaration == null || typeDeclaration.isShadow();
            String annotationTypeId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(annotationTypeName)
                    .isShadow(isShadow)
                    .build());
            String fullName = executableSignature + "#" + annotationType.getSimpleName();
            String annotationId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(fullName)
                    .isShadow(false)
                    .build());

            boolean isMarker = annotation.getValues().isEmpty();
            AnnotationNode annotationNode = AnnotationNode.builder()
                    .id(annotationId)
                    .nodeType(isMarker ? NodeType.MARKER_ANNOTATION : NodeType.ANNOTATION)
                    .fullName(fullName)
                    .name(annotationType.getSimpleName())
                    .type(isMarker ? NodeType.MARKER_ANNOTATION.name() : NodeType.ANNOTATION.name())
                    .lineStart(annotation.getPosition().isValidPosition() ? annotation.getPosition().getLine() : -1)
                    .lineEnd(annotation.getPosition().isValidPosition() ? annotation.getPosition().getEndLine() : -1)
                    .branchName(config.getBranch())
                    .commitStatus("COMMITTED")
                    .lastUpdated(now())
                    .repoId(config.getProjectId())
                    .build();
            annotationNode.setContent(annotation.toString());
            processNode(annotationNode);

            processEdge(Edge.builder()
                    .srcId(executableId)
                    .dstId(annotationId)
                    .type(EdgeType.CONTAINS)
                    .build());

            if (!ParseSupport.isIgnoreType(annotationType)) {
                processEdge(Edge.builder()
                        .srcId(annotationId)
                        .dstId(annotationTypeId)
                        .type(EdgeType.INSTANCE_OF)
                        .lineNumber(annotation.getPosition().isValidPosition() ? annotation.getPosition().getLine() : -1)
                        .build());
            }
        }
    }

    protected void processExecutableParameterDependencies(T element, String executableId) {
        for (CtParameter<?> parameter : element.getParameters()) {
            CtTypeReference<?> parameterType = parameter.getType();
            if (ParseSupport.isIgnoreType(parameterType)) {
                continue;
            }
            CtType<?> typeDeclaration = parameterType.getTypeDeclaration();
            boolean isShadow = typeDeclaration == null || typeDeclaration.isShadow();
            String parameterTypeId = IdGenerator.generate(IdGenerator.IdGeneratorContext.builder()
                    .fullQualifiedName(parameterType.getQualifiedName())
                    .isShadow(isShadow)
                    .build());
            processEdge(Edge.builder()
                    .srcId(executableId)
                    .dstId(parameterTypeId)
                    .type(EdgeType.DEPENDS_ON)
                    .dependencyType(DependencyType.USAGE)
                    .lineNumber(parameter.getPosition().isValidPosition() ? parameter.getPosition().getLine() : -1)
                    .build());
        }
    }

    protected void processExecutableCallDependencies(T element, String executableId) {
        element.getElements(new TypeFilter<>(CtInvocation.class)).forEach(invocation -> {
            CtExecutableReference<?> executableRef = invocation.getExecutable();
            if (executableRef == null || executableRef.getDeclaringType() == null) {
                return;
            }

            CtTypeReference<?> declaringType = executableRef.getDeclaringType();
            if (ParseSupport.isIgnoreType(declaringType)) {
                return;
            }

            CtExecutable<?> refExecutable = executableRef.getExecutableDeclaration();
            if (refExecutable == null) {
                getLogger().debug("Invocation {} could not resolve executable declaration", invocation);
                return;
            }
            if (ParseSupport.isSimpleGetter(refExecutable) || ParseSupport.isSimpleSetter(refExecutable)) {
                return;
            }

            CtType<?> typeDeclaration = declaringType.getTypeDeclaration();
            String targetClassName = declaringType.getQualifiedName();
            String targetMethodFullName = ParseSupport.getExecutableSignature(targetClassName, refExecutable);
            boolean isShadow = typeDeclaration == null || typeDeclaration.isShadow();
            String targetMethodId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(targetMethodFullName)
                    .isShadow(isShadow)
                    .build());

            CtExpression<?> target = invocation.getTarget();
            String actualTargetClass = targetClassName;
            boolean isInjection = false;
            ResolvedInjectionPoint resolvedInjectionPoint = null;
            CallSite callSite = buildCallSite(executableId, getDeclaringType(element), invocation, targetClassName);
            if (target != null) {
                String fieldName = getFieldNameFromExpression(target);
                if (fieldName != null) {
                    CtType<?> declaringExecutableType = getDeclaringType(element);
                    resolvedInjectionPoint = injectionSupport.resolveFieldInjection(declaringExecutableType, fieldName)
                            .orElse(null);
                    String implementationType = resolvedInjectionPoint != null
                            ? resolvedInjectionPoint.resolution().getResolvedTypeName()
                            : null;
                    if (implementationType != null) {
                        actualTargetClass = implementationType;
                        isInjection = true;
                    }
                } else if (target instanceof CtThisAccess<?>) {
                    actualTargetClass = getDeclaringType(element).getQualifiedName();
                } else if (target instanceof CtSuperAccess<?>) {
                    CtTypeReference<?> superclass = getDeclaringType(element).getSuperclass();
                    if (superclass != null) {
                        actualTargetClass = superclass.getQualifiedName();
                    }
                }
            }

            if (isInjection && resolvedInjectionPoint != null) {
                String finalTargetMethodId = targetMethodId;
                CtType<?> implementationType = getType(actualTargetClass);
                if (implementationType != null) {
                    CtMethod<?> implementationMethod = null;
                    CtExecutable<?> refDeclaration = executableRef.getDeclaration();
                    if (refDeclaration instanceof CtMethod<?> interfaceMethod) {
                        for (CtMethod<?> methodInImplementation : implementationType.getAllMethods()) {
                            if (methodInImplementation.isOverriding(interfaceMethod)) {
                                implementationMethod = methodInImplementation;
                                break;
                            }
                        }
                    }
                    if (implementationMethod != null) {
                        String actualMethodSignature = ParseSupport.getExecutableSignature(actualTargetClass, implementationMethod);
                        finalTargetMethodId = IdGenerator.generate(IdGenerator.builder()
                                .fullQualifiedName(actualMethodSignature)
                                .isShadow(implementationType.isShadow())
                                .build());
                    }
                }
                DependencyInjectionResolution resolution = resolvedInjectionPoint.resolution();
                InjectionPoint injectionPoint = resolvedInjectionPoint.injectionPoint();
                DependencyInjectionFact fact = DependencyInjectionFact.fromCallRetarget(
                        injectionPoint,
                        resolution,
                        callSite);
                dependencyInjectionEnhancer.createInjectionCallEdge(fact, executableId, finalTargetMethodId)
                        .ifPresent(this::processEdge);
                return;
            }

            if (isShadow) {
                processEdge(Edge.builder()
                        .srcId(executableId)
                        .dstId(targetMethodId)
                        .type(EdgeType.OUT_CALLS)
                        .lineNumber(invocation.getPosition().isValidPosition() ? invocation.getPosition().getLine() : -1)
                        .build());
                return;
            }

            processEdge(Edge.builder()
                    .srcId(executableId)
                    .dstId(targetMethodId)
                    .type(determineCallType(element, executableRef, actualTargetClass))
                    .lineNumber(invocation.getPosition().isValidPosition() ? invocation.getPosition().getLine() : -1)
                    .build());
        });
    }

    private EdgeType determineCallType(T caller, CtExecutableReference<?> callee, String actualTargetClass) {
        CtType<?> callerType = getDeclaringType(caller);
        String callerClassName = callerType.getQualifiedName();
        if (callerClassName.equals(actualTargetClass)) {
            return EdgeType.CALLS;
        }
        CtTypeReference<?> superclass = callerType.getSuperclass();
        if (superclass != null && superclass.getQualifiedName().equals(actualTargetClass)) {
            return EdgeType.SUPER_CALLS;
        }
        for (CtTypeReference<?> interfaceType : callerType.getSuperInterfaces()) {
            if (interfaceType.getQualifiedName().equals(actualTargetClass)
                    && callerType.isSubtypeOf(callee.getDeclaringType())) {
                return EdgeType.INTERFACE_CALLS;
            }
        }
        if (callerType.isSubtypeOf(callee.getDeclaringType())) {
            return EdgeType.SUBTYPE_CALLS;
        }
        return EdgeType.CALLS;
    }

    private CallSite buildCallSite(String callerId,
                                   CtType<?> ownerType,
                                   CtInvocation<?> invocation,
                                   String declaredTargetType) {
        CtExpression<?> target = invocation.getTarget();
        List<String> argumentTypes = new ArrayList<>();
        invocation.getArguments().forEach(argument -> {
            if (argument.getType() != null && argument.getType().getQualifiedName() != null) {
                argumentTypes.add(argument.getType().getQualifiedName());
            }
        });
        int lineNumber = invocation.getPosition().isValidPosition() ? invocation.getPosition().getLine() : -1;
        String location = ownerType.getQualifiedName() + ":" + lineNumber;
        return CallSite.builder()
                .callerId(callerId)
                .receiverSymbol(target == null ? null : target.toString())
                .declaredReceiverType(declaredTargetType)
                .methodName(invocation.getExecutable() != null ? invocation.getExecutable().getSimpleName() : null)
                .argumentTypes(argumentTypes)
                .location(location)
                .lineNumber(lineNumber)
                .build();
    }

    private String getFieldNameFromExpression(CtExpression<?> expression) {
        if (expression instanceof CtFieldAccess<?> fieldAccess) {
            return fieldAccess.getVariable().getSimpleName();
        }
        if (expression instanceof CtVariableAccess<?> variableAccess) {
            return variableAccess.getVariable().getSimpleName();
        }
        return null;
    }

    private CtType<?> getType(String typeName) {
        try {
            return getFactory().Type().get(typeName);
        } catch (Exception e) {
            getLogger().warn("Failed to resolve type: {}", typeName, e);
            return null;
        }
    }

    protected FunctionNode createFunctionNode(T element, String executableId, String executableName,
                                              String fullName, String visibility) {
        boolean isEntryPoint = determineEntryPoint(element);
        FunctionNode functionNode = FunctionNode.builder()
                .id(executableId)
                .nodeType(NodeType.FUNCTION)
                .fullName(fullName)
                .name(executableName)
                .visibility(visibility)
                .isStatic(isStatic(element))
                .isConstructor(isConstructor(element))
                .isLibrary(ParseType.LIBRARY.equals(parseType))
                .isEntryPoint(isEntryPoint)
                .lineStart(element.getPosition().isValidPosition() ? element.getPosition().getLine() : -1)
                .lineEnd(element.getPosition().isValidPosition() ? element.getPosition().getEndLine() : -1)
                .complexity(calculateComplexity(element))
                .branchName(config.getBranch())
                .commitStatus("COMMITTED")
                .lastUpdated(now())
                .repoId(config.getProjectId())
                .build();
        functionNode.setContent(element.toString());
        return functionNode;
    }

    protected boolean determineEntryPoint(T element) {
        try {
            if (!(element instanceof CtMethod<?> ctMethod)) {
                return false;
            }
            CtType<?> declaringType = getDeclaringType(element);
            if (declaringType == null) {
                return false;
            }
            RuleContext ruleContext = SpoonRuleContextBuilder.buildFromSpoon(ctMethod, declaringType);
            RuleEngine<CtExecutable<?>, Boolean> entryPointEngine = RuleEngineFactory.getInstance()
                    .getEngine(RuleEngineType.JAVA_ENTRY_POINT);
            if (entryPointEngine == null) {
                getLogger().warn("Entry point rule engine not found");
                return false;
            }
            return Boolean.TRUE.equals(entryPointEngine.execute(element, ruleContext));
        } catch (Exception e) {
            getLogger().warn("Failed to determine entry point for {}", element, e);
            return false;
        }
    }

    protected String determineVisibility(T element) {
        return "default";
    }

    protected abstract CtType<?> getDeclaringType(T element);

    protected abstract String getExecutableName(T element, CtType<?> declaringType);

    protected abstract boolean isConstructor(T element);

    protected abstract boolean isStatic(T element);

    protected int calculateComplexity(T element) {
        int complexity = 1;
        try {
            if (element.getBody() == null) {
                return complexity;
            }
            complexity += element.getElements(new TypeFilter<>(CtIf.class)).size();
            complexity += element.getElements(new TypeFilter<>(CtCase.class)).size();
            complexity += element.getElements(new TypeFilter<>(CtCatch.class)).size();
            complexity += element.getElements(new TypeFilter<>(CtFor.class)).size();
            complexity += element.getElements(new TypeFilter<>(CtForEach.class)).size();
            complexity += element.getElements(new TypeFilter<>(CtWhile.class)).size();
            complexity += element.getElements(new TypeFilter<>(CtDo.class)).size();
        } catch (Exception e) {
            getLogger().error("Failed to calculate complexity for {}", element, e);
        }
        return complexity;
    }

    public void processOverride(T element, String executableId) {
        if (!(element instanceof CtMethod<?> currentMethod)) {
            return;
        }

        Collection<CtMethod<?>> topDefinitions = currentMethod.getTopDefinitions();
        for (CtMethod<?> topDefinition : topDefinitions) {
            if (topDefinition.equals(currentMethod) || !currentMethod.getSignature().equals(topDefinition.getSignature())) {
                continue;
            }

            CtType<?> declaringType = topDefinition.getDeclaringType();
            String topMethodSignature = ParseSupport.getExecutableSignature(declaringType.getQualifiedName(), topDefinition);
            String topMethodId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(topMethodSignature)
                    .isShadow(declaringType.isShadow())
                    .build());

            EdgeType edgeType;
            if (declaringType.isInterface()) {
                edgeType = EdgeType.IMPLEMENTED_BY;
            } else {
                if (!isValidOverride(currentMethod, topDefinition)) {
                    continue;
                }
                edgeType = EdgeType.OVERRIDE;
            }

            processEdge(Edge.builder()
                    .srcId(topMethodId)
                    .dstId(executableId)
                    .type(edgeType)
                    .build());
        }
    }

    private boolean isValidOverride(CtMethod<?> current, CtMethod<?> parent) {
        if (parent.isFinal()) {
            return false;
        }
        if (!current.getType().isSubtypeOf(parent.getType())) {
            return false;
        }
        return !current.isPrivate() || parent.isPrivate();
    }
}
