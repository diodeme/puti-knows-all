package com.puti.code.analyzer.java.processor;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInjectionEnhancerTest {

    private final DependencyInjectionEnhancer enhancer = new DependencyInjectionEnhancer();

    @Test
    void shouldCreateBindingEdgeUsingLogicalEdgeResolver() {
        DependencyInjectionFact fact = DependencyInjectionFact.builder()
                .factKind(DependencyInjectionFactKinds.BINDING)
                .requestedName("userService")
                .requestedNamePresent(true)
                .candidateMatched(true)
                .injectionMode("resource")
                .resolutionKind("requestedName")
                .resolvedTypeName("com.demo.UserService")
                .annotations(List.of())
                .annotationAttributes(new LinkedHashMap<>(Map.of("requestedName", "userService")))
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .parameterTypes(List.of())
                .lineNumber(18)
                .build();

        var edgeOptional = enhancer.createBindingEdge(fact, "field-1", "class-1");

        assertTrue(edgeOptional.isPresent());
        assertEquals("instance_of", edgeOptional.get().getTypeName());
        assertEquals(18, edgeOptional.get().resolvedProperties().get("line_number"));
        assertEquals("requestedName", edgeOptional.get().resolvedProperties().get("resolution_kind"));
    }

    @Test
    void shouldCreateInjectionCallEdgeUsingLogicalEdgeResolver() {
        DependencyInjectionFact fact = DependencyInjectionFact.builder()
                .factKind(DependencyInjectionFactKinds.CALL_RETARGET)
                .candidateMatched(true)
                .injectionMode("autowired")
                .resolutionKind("priority")
                .resolvedTypeName("com.demo.UserService")
                .declaredTargetTypeName("com.demo.IUserService")
                .annotations(List.of())
                .annotationAttributes(Map.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .parameterTypes(List.of("java.lang.String"))
                .lineNumber(32)
                .build();

        var edgeOptional = enhancer.createInjectionCallEdge(fact, "method-1", "method-2");

        assertTrue(edgeOptional.isPresent());
        assertEquals("injection_calls", edgeOptional.get().getTypeName());
        assertEquals("com.demo.IUserService",
                edgeOptional.get().resolvedProperties().get("declared_target_type"));
        assertEquals(32, edgeOptional.get().resolvedProperties().get("line_number"));
    }
}
