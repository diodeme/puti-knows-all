package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.regex.Pattern;

import static com.puti.code.analyzer.java.support.ParseSupport.getExecutableSignature;

@Slf4j
public class DataLineageProcessor extends BaseProcessor<CtType<?>> {

    private final List<Pattern> entityPatterns;
    private final List<Pattern> mappingMethods;
    private final List<Pattern> accessorSetters;
    private final List<Pattern> accessorGetters;

    public DataLineageProcessor(GraphContext graphContext) {
        super(graphContext);
        AppConfig appConfig = AppConfig.getInstance();
        this.entityPatterns = appConfig.getLineageEntityPatterns().stream().map(Pattern::compile).toList();
        this.mappingMethods = appConfig.getLineageMappingMethods().stream().map(Pattern::compile).toList();
        this.accessorSetters = appConfig.getLineageAccessorSetters().stream().map(Pattern::compile).toList();
        this.accessorGetters = appConfig.getLineageAccessorGetters().stream().map(Pattern::compile).toList();
    }

    @Override
    public void process(CtType<?> element) {
        try {
            for (CtMethod<?> method : element.getMethods()) {
                String callerMethodId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(getExecutableSignature(element.getQualifiedName(), method))
                        .isShadow(false)
                        .build());

                List<CtInvocation<?>> invocations = method.getElements(new TypeFilter<>(CtInvocation.class));
                for (CtInvocation<?> invocation : invocations) {
                    processInvocation(invocation, callerMethodId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process data lineage for type: {}", element.getQualifiedName(), e);
        }
    }

    private void processInvocation(CtInvocation<?> invocation, String callerMethodId) {
        if (invocation.getExecutable() == null) {
            return;
        }

        String methodName = invocation.getExecutable().getSimpleName();
        CtTypeReference<?> declaringType = invocation.getExecutable().getDeclaringType();
        if (declaringType == null) {
            return;
        }

        if (isEntityLike(declaringType.getQualifiedName())) {
            if (matchesAny(methodName, accessorSetters)) {
                String fieldName = extractFieldName(methodName, "set");
                createFieldEdge(callerMethodId, declaringType.getQualifiedName(), fieldName,
                        EdgeType.WRITES_FIELD, invocation.getPosition().getLine());
            } else if (matchesAny(methodName, accessorGetters)) {
                String fieldName = extractFieldName(methodName, methodName.startsWith("is") ? "is" : "get");
                createFieldEdge(callerMethodId, declaringType.getQualifiedName(), fieldName,
                        EdgeType.READS_FIELD, invocation.getPosition().getLine());
            }
        }

        if (isBeanCopyMethod(declaringType.getQualifiedName(), methodName) && invocation.getArguments().size() >= 2) {
            CtTypeReference<?> sourceClass = invocation.getArguments().get(0).getType();
            CtTypeReference<?> targetClass = invocation.getArguments().get(1).getType();
            if (sourceClass != null && targetClass != null) {
                String sourceClassId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(sourceClass.getQualifiedName())
                        .isShadow(false)
                        .build());
                String targetClassId = IdGenerator.generate(IdGenerator.builder()
                        .fullQualifiedName(targetClass.getQualifiedName())
                        .isShadow(false)
                        .build());

                processEdge(Edge.builder()
                        .srcId(sourceClassId)
                        .dstId(targetClassId)
                        .type(EdgeType.MAPS_TO)
                        .lineNumber(invocation.getPosition().getLine())
                        .build());
            }
        }

        String targetMethodId;
        if (invocation.getExecutable().getExecutableDeclaration() != null && declaringType.getTypeDeclaration() != null) {
            targetMethodId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(getExecutableSignature(
                            declaringType.getQualifiedName(),
                            invocation.getExecutable().getExecutableDeclaration()))
                    .isShadow(false)
                    .build());
        } else {
            targetMethodId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName(invocation.getExecutable().getSignature())
                    .isShadow(false)
                    .build());
        }

        List<CtTypeReference<?>> parameters = invocation.getExecutable().getParameters();
        for (CtTypeReference<?> paramType : parameters) {
            if (paramType != null && isEntityLike(paramType.getQualifiedName())) {
                processEdge(Edge.builder()
                        .srcId(callerMethodId)
                        .dstId(targetMethodId)
                        .type(EdgeType.PASSES_TO)
                        .lineNumber(invocation.getPosition().getLine())
                        .build());
                break;
            }
        }
    }

    private void createFieldEdge(String callerMethodId, String declaringTypeName, String fieldName,
                                 EdgeType edgeType, int line) {
        String fieldId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName(declaringTypeName + "#" + fieldName)
                .isShadow(false)
                .build());

        processEdge(Edge.builder()
                .srcId(callerMethodId)
                .dstId(fieldId)
                .type(edgeType)
                .lineNumber(line)
                .build());
    }

    private String extractFieldName(String methodName, String prefixToRemove) {
        if (methodName.startsWith(prefixToRemove) && methodName.length() > prefixToRemove.length()) {
            return methodName.substring(prefixToRemove.length(), prefixToRemove.length() + 1).toLowerCase()
                    + methodName.substring(prefixToRemove.length() + 1);
        }
        return methodName;
    }

    private boolean isEntityLike(String qualifiedName) {
        return qualifiedName != null && matchesAny(qualifiedName, entityPatterns);
    }

    private boolean isBeanCopyMethod(String className, String methodName) {
        return matchesAny(className + "." + methodName, mappingMethods);
    }

    private boolean matchesAny(String text, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected org.slf4j.Logger getLogger() {
        return log;
    }
}
