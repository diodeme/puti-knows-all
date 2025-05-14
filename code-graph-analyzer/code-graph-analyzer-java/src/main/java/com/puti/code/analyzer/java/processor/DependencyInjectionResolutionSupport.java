package com.puti.code.analyzer.java.processor;

import com.puti.code.rule.config.RuleConfiguration;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 共享的 DI 事实登记与解析支持，避免事实提取和调用重定向各写一套框架逻辑。
 */
@Slf4j
public class DependencyInjectionResolutionSupport {

    private final Map<String, ProviderCandidate> providerCandidates = new LinkedHashMap<>();
    private final Map<String, TypeRelation> typeRelations = new LinkedHashMap<>();
    private final RuleConfiguration.DependencyInjectionFactMapping factMapping;
    private final RuleConfiguration.DependencyInjectionResolutionRules resolutionRules;

    public DependencyInjectionResolutionSupport() {
        this(RuleConfiguration.getInstance().getDependencyInjectionRules());
    }

    DependencyInjectionResolutionSupport(RuleConfiguration.DependencyInjectionRules rules) {
        this.factMapping = rules != null && rules.getFactMapping() != null
                ? rules.getFactMapping()
                : new RuleConfiguration.DependencyInjectionFactMapping();
        this.resolutionRules = rules != null && rules.getResolutionRules() != null
                ? rules.getResolutionRules()
                : new RuleConfiguration.DependencyInjectionResolutionRules();
    }

    public void registerProviderCandidate(ProviderCandidate candidate) {
        if (candidate == null || candidate.getDeclaredType() == null || candidate.getActualType() == null) {
            return;
        }
        String key = candidate.getDeclaredType() + "->" + candidate.getActualType() + "#" + candidate.getProviderName();
        providerCandidates.put(key, candidate);
    }

    public List<TypeRelation> snapshotTypeRelations() {
        return new ArrayList<>(typeRelations.values());
    }

    public boolean isTypeProviderAnnotation(String annotationName) {
        return findProviderMapping(annotationName, "TYPE").isPresent();
    }

    public boolean isMethodProviderAnnotation(String annotationName) {
        return findProviderMapping(annotationName, "METHOD").isPresent();
    }

    public boolean isInjectionOrQualifierAnnotation(String annotationName) {
        return findInjectionAnnotationMapping(annotationName).isPresent()
                || findQualifierAnnotationMapping(annotationName).isPresent();
    }

    public boolean isManagedFrameworkAnnotation(String annotationName) {
        return isTypeProviderAnnotation(annotationName)
                || isMethodProviderAnnotation(annotationName)
                || isInjectionOrQualifierAnnotation(annotationName)
                || isPriorityAnnotation(annotationName);
    }

    public void registerTypeProvider(CtType<?> element, CtAnnotation<?> annotation, Map<String, Object> annotationValues) {
        if (element == null) {
            return;
        }
        RuleConfiguration.ProviderAnnotationMapping providerMapping =
                findProviderMapping(annotation.getAnnotationType().getQualifiedName(), "TYPE").orElse(null);
        if (providerMapping == null) {
            return;
        }
        String actualType = element.getQualifiedName();
        List<String> providerNames = extractProviderNames(actualType, annotationValues, providerMapping);
        Integer priority = resolvePriority(element);
        Set<String> exposedTypes = collectExposedTypes(element);

        if (providerNames.isEmpty()) {
            registerTypeProviderCandidates(exposedTypes, actualType, annotation, priority, null);
            return;
        }
        for (String exposedType : exposedTypes) {
            for (String providerName : providerNames) {
                registerProviderCandidate(ProviderCandidate.builder()
                        .sourceKind("class")
                        .declaredType(exposedType)
                        .actualType(actualType)
                        .providerName(providerName)
                        .qualifiers(List.of(providerName))
                        .priority(priority)
                        .frameworkHints(Map.of(
                                "providerAnnotation", annotation.getAnnotationType().getQualifiedName(),
                                "componentType", actualType))
                        .build());
            }
        }
    }

    public void registerMethodProvider(CtMethod<?> method, CtAnnotation<?> providerAnnotation) {
        if (method == null || method.getType() == null) {
            return;
        }
        RuleConfiguration.ProviderAnnotationMapping providerMapping =
                findProviderMapping(providerAnnotation.getAnnotationType().getQualifiedName(), "METHOD").orElse(null);
        if (providerMapping == null) {
            return;
        }
        String declaredType = method.getType().getQualifiedName();
        String actualType = inferReturnedInstantiationType(method).orElse(declaredType);
        recordTypeRelation(actualType, declaredType, "assignable_to");
        CtType<?> actualTypeDeclaration = method.getFactory().Type().get(actualType);
        if (actualTypeDeclaration != null) {
            collectExposedTypes(actualTypeDeclaration);
        }
        List<String> providerNames = extractMethodProviderNames(method, providerAnnotation, providerMapping);
        Integer priority = resolvePriority(method);

        if (providerNames.isEmpty()) {
            registerMethodProviderCandidate(method, providerAnnotation, declaredType, actualType, priority, null);
            return;
        }
        for (String providerName : providerNames) {
            registerMethodProviderCandidate(method, providerAnnotation, declaredType, actualType, priority, providerName);
        }
    }

    public Optional<InjectionPoint> extractInjectionPoint(CtType<?> ownerType, CtField<?> field) {
        if (ownerType == null || field == null || field.getType() == null) {
            return Optional.empty();
        }

        boolean matchedInjectionAnnotation = false;
        String requestedName = null;
        List<String> qualifiers = new ArrayList<>();
        String injectionMode = null;
        String injectionAnnotation = null;

        for (CtAnnotation<?> annotation : field.getAnnotations()) {
            String annotationName = annotation.getAnnotationType().getQualifiedName();
            RuleConfiguration.InjectionAnnotationMapping injectionMapping = findInjectionAnnotationMapping(annotationName).orElse(null);
            if (injectionMapping != null) {
                injectionMode = injectionMapping.getMode();
                injectionAnnotation = annotationName;
                matchedInjectionAnnotation = true;
                if (hasText(injectionMapping.getNameAttribute())) {
                    String explicitName = resolveAnnotationAttribute(
                            annotation.getValues().get(injectionMapping.getNameAttribute()), ownerType);
                    if (hasText(explicitName)) {
                        requestedName = explicitName;
                    }
                }
                continue;
            }
            RuleConfiguration.QualifierAnnotationMapping qualifierMapping = findQualifierAnnotationMapping(annotationName).orElse(null);
            if (qualifierMapping != null) {
                String qualifier = resolveAnnotationAttribute(annotation.getValues().get(qualifierMapping.getAttribute()), ownerType);
                if (qualifier != null && !qualifier.isBlank()) {
                    qualifiers.add(qualifier);
                    requestedName = qualifier;
                }
            }
        }

        if (!matchedInjectionAnnotation) {
            return Optional.empty();
        }

        return Optional.of(InjectionPoint.builder()
                .ownerTypeName(ownerType.getQualifiedName())
                .memberName(field.getSimpleName())
                .memberKind("field")
                .requiredType(field.getType().getQualifiedName())
                .requestedName(requestedName)
                .qualifiers(qualifiers)
                .required(true)
                .injectionMode(hasText(injectionMode) ? injectionMode : "injection")
                .frameworkHints(buildInjectionFrameworkHints(
                        ownerType,
                        injectionAnnotation,
                        field.getAnnotations().stream()
                                .map(annotation -> annotation.getAnnotationType().getQualifiedName())
                                .filter(this::isInjectionOrQualifierAnnotation)
                                .toList()))
                .build());
    }

    public DependencyInjectionResolution resolve(InjectionPoint injectionPoint) {
        if (injectionPoint == null || injectionPoint.getRequiredType() == null) {
            return DependencyInjectionResolution.builder().candidateMatched(false).build();
        }

        List<ProviderCandidate> candidates = providerCandidates.values().stream()
                .filter(candidate -> candidate.supportsType(injectionPoint.getRequiredType()))
                .toList();
        boolean requestedNamePresent = hasText(injectionPoint.getRequestedName());

        for (String strategy : configuredResolutionOrder()) {
            ProviderCandidate matchedCandidate = resolveByStrategy(candidates, injectionPoint, strategy);
            if (matchedCandidate == null) {
                continue;
            }
            return resolved(
                    matchedCandidate,
                    strategy,
                    requestedNamePresent,
                    "priority".equals(strategy),
                    "uniqueCandidate".equals(strategy));
        }

        return DependencyInjectionResolution.builder()
                .candidateMatched(false)
                .requestedNamePresent(requestedNamePresent)
                .build();
    }

    public Optional<String> resolveFieldTargetType(CtType<?> ownerType, String fieldName) {
        if (ownerType == null || fieldName == null || fieldName.isBlank()) {
            return Optional.empty();
        }
        return resolveFieldInjection(ownerType, fieldName)
                .map(ResolvedInjectionPoint::resolution)
                .filter(DependencyInjectionResolution::isCandidateMatched)
                .map(DependencyInjectionResolution::getResolvedTypeName);
    }

    public Optional<ResolvedInjectionPoint> resolveFieldInjection(CtType<?> ownerType, String fieldName) {
        if (ownerType == null || fieldName == null || fieldName.isBlank()) {
            return Optional.empty();
        }
        CtField<?> field = findFieldInHierarchy(ownerType, fieldName);
        if (field == null) {
            return Optional.empty();
        }
        CtType<?> fieldOwnerType = field.getDeclaringType() != null ? field.getDeclaringType() : ownerType;
        return extractInjectionPoint(fieldOwnerType, field)
                .map(injectionPoint -> new ResolvedInjectionPoint(injectionPoint, resolve(injectionPoint)));
    }

    private DependencyInjectionResolution resolved(ProviderCandidate candidate, String resolutionKind,
                                                   boolean requestedNamePresent, boolean priorityMatched,
                                                   boolean uniqueCandidateMatched) {
        return DependencyInjectionResolution.builder()
                .candidate(candidate)
                .resolutionKind(resolutionKind)
                .candidateMatched(true)
                .requestedNamePresent(requestedNamePresent)
                .priorityCandidateMatched(priorityMatched)
                .uniqueCandidateMatched(uniqueCandidateMatched)
                .build();
    }

    private ProviderCandidate matchByName(List<ProviderCandidate> candidates, String candidateName) {
        if (!hasText(candidateName)) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> candidate.matchesName(candidateName))
                .findFirst()
                .orElse(null);
    }

    private ProviderCandidate matchByNames(List<ProviderCandidate> candidates, List<String> candidateNames) {
        if (candidateNames == null || candidateNames.isEmpty()) {
            return null;
        }
        for (String candidateName : candidateNames) {
            ProviderCandidate matchedCandidate = matchByName(candidates, candidateName);
            if (matchedCandidate != null) {
                return matchedCandidate;
            }
        }
        return null;
    }

    private ProviderCandidate matchByPriority(List<ProviderCandidate> candidates) {
        return candidates.stream()
                .filter(ProviderCandidate::hasPriority)
                .sorted((left, right) -> Integer.compare(
                        right.getPriority() != null ? right.getPriority() : 0,
                        left.getPriority() != null ? left.getPriority() : 0))
                .findFirst()
                .orElse(null);
    }

    private ProviderCandidate matchUniqueCandidate(List<ProviderCandidate> candidates) {
        Map<String, ProviderCandidate> uniqueByActualType = new LinkedHashMap<>();
        for (ProviderCandidate candidate : candidates) {
            uniqueByActualType.putIfAbsent(candidate.getActualType(), candidate);
        }
        return uniqueByActualType.size() == 1 ? uniqueByActualType.values().iterator().next() : null;
    }

    private ProviderCandidate resolveByStrategy(List<ProviderCandidate> candidates, InjectionPoint injectionPoint,
                                                String strategy) {
        return switch (strategy) {
            case "qualifier" -> matchByNames(candidates, injectionPoint.getQualifiers());
            case "requestedName" -> matchByName(candidates, injectionPoint.getRequestedName());
            case "memberName" -> matchByName(candidates, injectionPoint.getMemberName());
            case "requiredTypeDefaultName" -> matchByName(candidates,
                    defaultNameFromType(injectionPoint.getRequiredType()));
            case "priority" -> matchByPriority(candidates);
            case "uniqueCandidate" -> matchUniqueCandidate(candidates);
            default -> null;
        };
    }

    private List<String> extractProviderNames(String typeName, Map<String, Object> annotationValues,
                                              RuleConfiguration.ProviderAnnotationMapping providerMapping) {
        List<String> providerNames = new ArrayList<>();
        if (annotationValues != null) {
            for (String attribute : providerMapping.getNameAttributes()) {
                providerNames.addAll(expandValue(annotationValues.get(attribute)));
            }
        }
        if (providerNames.isEmpty()) {
            String fallbackName = resolveFallbackProviderName(providerMapping.getFallbackNameSource(), typeName, null);
            if (hasText(fallbackName)) {
                providerNames.add(fallbackName);
            }
        }
        return providerNames.stream()
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private List<String> extractMethodProviderNames(CtMethod<?> method, CtAnnotation<?> providerAnnotation,
                                                    RuleConfiguration.ProviderAnnotationMapping providerMapping) {
        Map<String, Object> annotationValues = extractAnnotationValues(providerAnnotation, method.getDeclaringType());
        List<String> providerNames = extractProviderNames(method.getType().getQualifiedName(), annotationValues, providerMapping);
        if (providerNames.isEmpty()) {
            String fallbackName = resolveFallbackProviderName(
                    providerMapping.getFallbackNameSource(),
                    method.getType().getQualifiedName(),
                    method.getSimpleName());
            if (hasText(fallbackName)) {
                return List.of(fallbackName);
            }
        }
        return providerNames;
    }

    private Optional<String> inferReturnedInstantiationType(CtMethod<?> method) {
        if (method.getBody() == null) {
            return Optional.empty();
        }
        List<CtReturn<?>> returns = method.getElements(new TypeFilter<>(CtReturn.class));
        if (returns.size() != 1) {
            return Optional.empty();
        }
        CtExpression<?> returnedExpression = returns.getFirst().getReturnedExpression();
        if (returnedExpression instanceof CtConstructorCall<?> constructorCall && constructorCall.getType() != null) {
            return Optional.ofNullable(constructorCall.getType().getQualifiedName());
        }
        return Optional.empty();
    }

    private Integer resolvePriority(CtElement element) {
        if (element == null || factMapping.getPriorityAnnotations() == null) {
            return 0;
        }
        return element.getAnnotations().stream()
                .map(annotation -> annotation.getAnnotationType().getQualifiedName())
                .flatMap(annotationName -> factMapping.getPriorityAnnotations().stream()
                        .filter(mapping -> annotationName.equals(mapping.getAnnotation())))
                .map(RuleConfiguration.PriorityAnnotationMapping::getPriority)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private String resolveAnnotationAttribute(Object value, CtType<?> declaringType) {
        List<String> values = expandExpression(value, declaringType);
        return values.isEmpty() ? null : values.getFirst();
    }

    private Map<String, Object> extractAnnotationValues(CtAnnotation<?> annotation, CtType<?> declaringType) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (annotation == null) {
            return values;
        }
        annotation.getValues().forEach((key, value) -> values.put(key, expandExpression(value, declaringType)));
        Map<String, Object> flattened = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof List<?> list && list.size() == 1) {
                flattened.put(key, list.getFirst());
            } else {
                flattened.put(key, value);
            }
        });
        return flattened;
    }

    private List<String> expandExpression(Object value, CtType<?> declaringType) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof CtNewArray<?> newArray) {
            List<String> resolved = new ArrayList<>();
            for (CtExpression<?> expression : newArray.getElements()) {
                resolved.addAll(expandExpression(expression, declaringType));
            }
            return resolved;
        }
        if (value instanceof CtFieldAccess<?> fieldAccess) {
            return List.of(resolveFieldAccessValue(fieldAccess));
        }
        if (value instanceof CtVariableAccess<?> variableAccess) {
            return List.of(resolveVariableAccessValue(variableAccess, declaringType));
        }
        if (value instanceof CtLiteral<?> literal) {
            return List.of(String.valueOf(literal.getValue()));
        }
        if (value instanceof CtExpression<?> expression) {
            return List.of(String.valueOf(expression));
        }
        return expandValue(value);
    }

    private List<String> expandValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            List<String> values = new ArrayList<>();
            for (Object item : collection) {
                values.addAll(expandValue(item));
            }
            return values;
        }
        if (value.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            Object[] array = (Object[]) value;
            for (Object item : array) {
                values.addAll(expandValue(item));
            }
            return values;
        }
        return List.of(String.valueOf(value));
    }

    private String resolveFieldAccessValue(CtFieldAccess<?> fieldAccess) {
        try {
            CtTypeReference<?> declaringType = fieldAccess.getVariable().getDeclaringType();
            if (declaringType == null) {
                return fieldAccess.getVariable().getSimpleName();
            }
            CtType<?> type = declaringType.getTypeDeclaration();
            if (type == null) {
                return fieldAccess.getVariable().getSimpleName();
            }
            CtField<?> field = type.getField(fieldAccess.getVariable().getSimpleName());
            if (field != null && field.getDefaultExpression() instanceof CtLiteral<?> literal) {
                return String.valueOf(literal.getValue());
            }
        } catch (Exception e) {
            log.debug("Failed to resolve field access annotation value", e);
        }
        return fieldAccess.getVariable().getSimpleName();
    }

    private String resolveVariableAccessValue(CtVariableAccess<?> variableAccess, CtType<?> declaringType) {
        try {
            if (declaringType != null) {
                CtField<?> field = declaringType.getField(variableAccess.getVariable().getSimpleName());
                if (field != null && field.getDefaultExpression() instanceof CtLiteral<?> literal) {
                    return String.valueOf(literal.getValue());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve variable access annotation value", e);
        }
        return variableAccess.getVariable().getSimpleName();
    }

    private void registerTypeProviderCandidates(Set<String> exposedTypes, String actualType, CtAnnotation<?> annotation,
                                                Integer priority, String providerName) {
        for (String exposedType : exposedTypes) {
            registerProviderCandidate(ProviderCandidate.builder()
                    .sourceKind("class")
                    .declaredType(exposedType)
                    .actualType(actualType)
                    .providerName(providerName)
                    .qualifiers(hasText(providerName) ? List.of(providerName) : List.of())
                    .priority(priority)
                    .frameworkHints(Map.of(
                            "providerAnnotation", annotation.getAnnotationType().getQualifiedName(),
                            "componentType", actualType))
                    .build());
        }
    }

    private void registerMethodProviderCandidate(CtMethod<?> method, CtAnnotation<?> providerAnnotation,
                                                 String declaredType, String actualType, Integer priority,
                                                 String providerName) {
        registerProviderCandidate(ProviderCandidate.builder()
                .sourceKind("beanMethod")
                .declaredType(declaredType)
                .actualType(actualType)
                .providerName(providerName)
                .qualifiers(hasText(providerName) ? List.of(providerName) : List.of())
                .priority(priority)
                .frameworkHints(Map.of(
                        "providerAnnotation", providerAnnotation.getAnnotationType().getQualifiedName(),
                        "beanMethod", method.getSimpleName()))
                .build());
    }

    private Set<String> collectExposedTypes(CtType<?> type) {
        Set<String> exposedTypes = new LinkedHashSet<>();
        if (type == null || !hasText(type.getQualifiedName())) {
            return exposedTypes;
        }
        String currentType = type.getQualifiedName();
        exposedTypes.add(currentType);
        collectInterfaceHierarchy(currentType, type.getSuperInterfaces(), exposedTypes);

        CtTypeReference<?> superclass = type.getSuperclass();
        while (superclass != null && hasText(superclass.getQualifiedName())
                && !"java.lang.Object".equals(superclass.getQualifiedName())) {
            exposedTypes.add(superclass.getQualifiedName());
            recordTypeRelation(currentType, superclass.getQualifiedName(), "extends");
            CtType<?> superclassType = superclass.getTypeDeclaration();
            if (superclassType == null) {
                break;
            }
            collectInterfaceHierarchy(superclass.getQualifiedName(), superclassType.getSuperInterfaces(), exposedTypes);
            superclass = superclassType.getSuperclass();
        }
        return exposedTypes;
    }

    private void collectInterfaceHierarchy(String sourceType,
                                           Collection<CtTypeReference<?>> interfaceTypes,
                                           Set<String> exposedTypes) {
        if (interfaceTypes == null) {
            return;
        }
        for (CtTypeReference<?> interfaceType : interfaceTypes) {
            if (interfaceType == null || !hasText(interfaceType.getQualifiedName())) {
                continue;
            }
            exposedTypes.add(interfaceType.getQualifiedName());
            recordTypeRelation(sourceType, interfaceType.getQualifiedName(), "implements");
            CtType<?> interfaceDeclaration = interfaceType.getTypeDeclaration();
            if (interfaceDeclaration != null) {
                collectInterfaceHierarchy(interfaceType.getQualifiedName(),
                        interfaceDeclaration.getSuperInterfaces(),
                        exposedTypes);
            }
        }
    }

    private void recordTypeRelation(String sourceType, String targetType, String relationKind) {
        if (!hasText(sourceType) || !hasText(targetType) || sourceType.equals(targetType)) {
            return;
        }
        String key = sourceType + "->" + targetType + "#" + relationKind;
        typeRelations.putIfAbsent(key, TypeRelation.builder()
                .sourceType(sourceType)
                .targetType(targetType)
                .relationKind(relationKind)
                .build());
    }

    private CtField<?> findFieldInHierarchy(CtType<?> ownerType, String fieldName) {
        CtType<?> currentType = ownerType;
        while (currentType != null) {
            CtField<?> field = currentType.getField(fieldName);
            if (field != null) {
                return field;
            }
            CtTypeReference<?> superclass = currentType.getSuperclass();
            currentType = superclass != null ? superclass.getTypeDeclaration() : null;
        }
        return null;
    }

    private String defaultNameFromType(String typeName) {
        if (!hasText(typeName)) {
            return typeName;
        }
        String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
        if (simpleName.isEmpty()) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private String resolveFallbackProviderName(String fallbackNameSource, String typeName, String methodName) {
        if (!hasText(fallbackNameSource) || "none".equalsIgnoreCase(fallbackNameSource)) {
            return null;
        }
        return switch (fallbackNameSource) {
            case "typeSimpleNameLowerCamel" -> defaultNameFromType(typeName);
            case "methodName" -> methodName;
            default -> null;
        };
    }

    private List<String> configuredResolutionOrder() {
        if (resolutionRules.getOrder() == null || resolutionRules.getOrder().isEmpty()) {
            return List.of("qualifier", "requestedName", "memberName", "requiredTypeDefaultName", "priority", "uniqueCandidate");
        }
        return resolutionRules.getOrder();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> buildInjectionFrameworkHints(CtType<?> ownerType,
                                                             String injectionAnnotation,
                                                             List<String> injectionAnnotations) {
        Map<String, Object> hints = new LinkedHashMap<>();
        if (hasText(injectionAnnotation)) {
            hints.put("injectionAnnotation", injectionAnnotation);
        }
        if (injectionAnnotations != null && !injectionAnnotations.isEmpty()) {
            hints.put("injectionAnnotations", injectionAnnotations.stream().distinct().toList());
        }
        if (ownerType != null) {
            List<String> classAnnotations = ownerType.getAnnotations().stream()
                    .map(annotation -> annotation.getAnnotationType().getQualifiedName())
                    .filter(this::hasText)
                    .toList();
            if (!classAnnotations.isEmpty()) {
                hints.put("ownerClassAnnotations", classAnnotations);
            }

            List<String> implementedInterfaces = ownerType.getSuperInterfaces().stream()
                    .map(CtTypeReference::getQualifiedName)
                    .filter(this::hasText)
                    .toList();
            if (!implementedInterfaces.isEmpty()) {
                hints.put("ownerImplementedInterfaces", implementedInterfaces);
            }

            List<String> superClassPath = new ArrayList<>();
            CtTypeReference<?> superclass = ownerType.getSuperclass();
            while (superclass != null && hasText(superclass.getQualifiedName())
                    && !"java.lang.Object".equals(superclass.getQualifiedName())) {
                superClassPath.add(superclass.getQualifiedName());
                CtType<?> superclassType = superclass.getTypeDeclaration();
                superclass = superclassType != null ? superclassType.getSuperclass() : null;
            }
            if (!superClassPath.isEmpty()) {
                hints.put("ownerSuperClassPath", superClassPath);
            }
        }
        return hints;
    }

    private Optional<RuleConfiguration.ProviderAnnotationMapping> findProviderMapping(String annotationName, String target) {
        if (annotationName == null || factMapping.getProviderAnnotations() == null) {
            return Optional.empty();
        }
        return factMapping.getProviderAnnotations().stream()
                .filter(mapping -> annotationName.equals(mapping.getAnnotation()))
                .filter(mapping -> target.equalsIgnoreCase(mapping.getTarget()))
                .findFirst();
    }

    private Optional<RuleConfiguration.InjectionAnnotationMapping> findInjectionAnnotationMapping(String annotationName) {
        if (annotationName == null || factMapping.getInjectionAnnotations() == null) {
            return Optional.empty();
        }
        return factMapping.getInjectionAnnotations().stream()
                .filter(mapping -> annotationName.equals(mapping.getAnnotation()))
                .findFirst();
    }

    private Optional<RuleConfiguration.QualifierAnnotationMapping> findQualifierAnnotationMapping(String annotationName) {
        if (annotationName == null || factMapping.getQualifierAnnotations() == null) {
            return Optional.empty();
        }
        return factMapping.getQualifierAnnotations().stream()
                .filter(mapping -> annotationName.equals(mapping.getAnnotation()))
                .findFirst();
    }

    private boolean isPriorityAnnotation(String annotationName) {
        if (annotationName == null || factMapping.getPriorityAnnotations() == null) {
            return false;
        }
        return factMapping.getPriorityAnnotations().stream()
                .anyMatch(mapping -> annotationName.equals(mapping.getAnnotation()));
    }
}
