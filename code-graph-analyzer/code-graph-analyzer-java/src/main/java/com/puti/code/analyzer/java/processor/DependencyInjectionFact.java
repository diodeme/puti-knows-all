package com.puti.code.analyzer.java.processor;

import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Builder
public record DependencyInjectionFact(
        String factKind,
        String ownerTypeName,
        String memberName,
        String memberKind,
        String requiredTypeName,
        String declaredTargetTypeName,
        String requestedName,
        boolean qualifierPresent,
        String injectionMode,
        String resolutionKind,
        String resolvedTypeName,
        boolean candidateMatched,
        boolean requestedNamePresent,
        boolean priorityCandidateMatched,
        boolean uniqueCandidateMatched,
        int lineNumber,
        List<String> annotations,
        Map<String, Object> annotationAttributes,
        List<String> classAnnotations,
        List<String> implementedInterfaces,
        List<String> superClassPath,
        String methodSignature,
        List<String> parameterTypes,
        String returnTypeName) {

    public static DependencyInjectionFact fromBinding(InjectionPoint injectionPoint,
                                                      DependencyInjectionResolution resolution,
                                                      int lineNumber) {
        return DependencyInjectionFact.builder()
                .factKind(DependencyInjectionFactKinds.BINDING)
                .ownerTypeName(injectionPoint.getOwnerTypeName())
                .memberName(injectionPoint.getMemberName())
                .memberKind(injectionPoint.getMemberKind())
                .requiredTypeName(injectionPoint.getRequiredType())
                .declaredTargetTypeName(injectionPoint.getRequiredType())
                .requestedName(injectionPoint.getRequestedName())
                .qualifierPresent(hasQualifiers(injectionPoint))
                .injectionMode(injectionPoint.getInjectionMode())
                .resolutionKind(resolution != null ? resolution.getResolutionKind() : null)
                .resolvedTypeName(resolution != null ? resolution.getResolvedTypeName() : null)
                .candidateMatched(resolution != null && resolution.isCandidateMatched())
                .requestedNamePresent(resolution != null && resolution.isRequestedNamePresent())
                .priorityCandidateMatched(resolution != null && resolution.isPriorityCandidateMatched())
                .uniqueCandidateMatched(resolution != null && resolution.isUniqueCandidateMatched())
                .lineNumber(lineNumber)
                .annotations(resolveAnnotations(injectionPoint))
                .annotationAttributes(resolveAnnotationAttributes(injectionPoint, null))
                .classAnnotations(resolveStringListHint(injectionPoint, "ownerClassAnnotations"))
                .implementedInterfaces(resolveStringListHint(injectionPoint, "ownerImplementedInterfaces"))
                .superClassPath(resolveStringListHint(injectionPoint, "ownerSuperClassPath"))
                .parameterTypes(List.of())
                .returnTypeName(resolution != null ? resolution.getResolvedTypeName() : injectionPoint.getRequiredType())
                .build();
    }

    public static DependencyInjectionFact fromCallRetarget(InjectionPoint injectionPoint,
                                                           DependencyInjectionResolution resolution,
                                                           CallSite callSite) {
        return DependencyInjectionFact.builder()
                .factKind(DependencyInjectionFactKinds.CALL_RETARGET)
                .ownerTypeName(injectionPoint.getOwnerTypeName())
                .memberName(injectionPoint.getMemberName())
                .memberKind(injectionPoint.getMemberKind())
                .requiredTypeName(injectionPoint.getRequiredType())
                .declaredTargetTypeName(callSite.getDeclaredReceiverType())
                .requestedName(injectionPoint.getRequestedName())
                .qualifierPresent(hasQualifiers(injectionPoint))
                .injectionMode(injectionPoint.getInjectionMode())
                .resolutionKind(resolution != null ? resolution.getResolutionKind() : null)
                .resolvedTypeName(resolution != null ? resolution.getResolvedTypeName() : null)
                .candidateMatched(resolution != null && resolution.isCandidateMatched())
                .requestedNamePresent(resolution != null && resolution.isRequestedNamePresent())
                .priorityCandidateMatched(resolution != null && resolution.isPriorityCandidateMatched())
                .uniqueCandidateMatched(resolution != null && resolution.isUniqueCandidateMatched())
                .lineNumber(callSite.getLineNumber() != null ? callSite.getLineNumber() : -1)
                .annotations(resolveAnnotations(injectionPoint))
                .annotationAttributes(resolveAnnotationAttributes(injectionPoint, callSite))
                .classAnnotations(resolveStringListHint(injectionPoint, "ownerClassAnnotations"))
                .implementedInterfaces(resolveStringListHint(injectionPoint, "ownerImplementedInterfaces"))
                .superClassPath(resolveStringListHint(injectionPoint, "ownerSuperClassPath"))
                .methodSignature(buildMethodSignature(injectionPoint, callSite))
                .parameterTypes(callSite != null && callSite.getArgumentTypes() != null ? callSite.getArgumentTypes() : List.of())
                .returnTypeName(resolution != null ? resolution.getResolvedTypeName() : injectionPoint.getRequiredType())
                .build();
    }

    private static boolean hasQualifiers(InjectionPoint injectionPoint) {
        return injectionPoint.getQualifiers() != null && !injectionPoint.getQualifiers().isEmpty();
    }

    private static List<String> resolveAnnotations(InjectionPoint injectionPoint) {
        return resolveStringListHint(injectionPoint, "injectionAnnotations");
    }

    @SuppressWarnings("unchecked")
    private static List<String> resolveStringListHint(InjectionPoint injectionPoint, String hintKey) {
        if (injectionPoint == null || injectionPoint.getFrameworkHints() == null) {
            return List.of();
        }
        Object value = injectionPoint.getFrameworkHints().get(hintKey);
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(item -> item != null && !item.isBlank())
                    .toList();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return List.of(stringValue);
        }
        return List.of();
    }

    private static Map<String, Object> resolveAnnotationAttributes(InjectionPoint injectionPoint, CallSite callSite) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (injectionPoint != null) {
            if (injectionPoint.getRequestedName() != null && !injectionPoint.getRequestedName().isBlank()) {
                attributes.put("requestedName", injectionPoint.getRequestedName());
            }
            if (injectionPoint.getQualifiers() != null && !injectionPoint.getQualifiers().isEmpty()) {
                attributes.put("qualifiers", injectionPoint.getQualifiers());
            }
            if (injectionPoint.getInjectionMode() != null && !injectionPoint.getInjectionMode().isBlank()) {
                attributes.put("injectionMode", injectionPoint.getInjectionMode());
            }
        }
        if (callSite != null && callSite.getMethodName() != null && !callSite.getMethodName().isBlank()) {
            attributes.put("methodName", callSite.getMethodName());
        }
        return attributes;
    }

    private static String buildMethodSignature(InjectionPoint injectionPoint, CallSite callSite) {
        if (injectionPoint == null || callSite == null || callSite.getMethodName() == null || callSite.getMethodName().isBlank()) {
            return null;
        }
        return injectionPoint.getOwnerTypeName() + "#" + callSite.getMethodName();
    }
}
