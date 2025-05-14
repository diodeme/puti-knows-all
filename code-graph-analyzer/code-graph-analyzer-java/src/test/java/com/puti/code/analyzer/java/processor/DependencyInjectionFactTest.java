package com.puti.code.analyzer.java.processor;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInjectionFactTest {

    @Test
    void shouldBuildCallRetargetFactFromTypedFacts() {
        InjectionPoint injectionPoint = InjectionPoint.builder()
                .ownerTypeName("demo.UserController")
                .memberName("userService")
                .memberKind("field")
                .requiredType("demo.IUserService")
                .requestedName("userService")
                .qualifiers(List.of("userService"))
                .injectionMode("autowired")
                .frameworkHints(new LinkedHashMap<>(Map.of(
                        "injectionAnnotations", List.of(
                                "org.springframework.beans.factory.annotation.Autowired",
                                "org.springframework.beans.factory.annotation.Qualifier"),
                        "ownerClassAnnotations", List.of("org.springframework.stereotype.Controller"),
                        "ownerImplementedInterfaces", List.of("demo.ControllerContract"),
                        "ownerSuperClassPath", List.of("demo.BaseController"))))
                .build();
        DependencyInjectionResolution resolution = DependencyInjectionResolution.builder()
                .candidate(ProviderCandidate.builder()
                        .declaredType("demo.IUserService")
                        .actualType("demo.UserService")
                        .providerName("userService")
                        .build())
                .resolutionKind("requestedName")
                .candidateMatched(true)
                .requestedNamePresent(true)
                .build();
        CallSite callSite = CallSite.builder()
                .callerId("caller-1")
                .receiverSymbol("userService")
                .declaredReceiverType("demo.IUserService")
                .methodName("query")
                .argumentTypes(List.of("java.lang.String"))
                .location("demo.UserController:32")
                .lineNumber(32)
                .build();

        DependencyInjectionFact fact = DependencyInjectionFact.fromCallRetarget(
                injectionPoint,
                resolution,
                callSite);

        assertEquals(DependencyInjectionFactKinds.CALL_RETARGET, fact.factKind());
        assertEquals("demo.IUserService", fact.declaredTargetTypeName());
        assertEquals("demo.UserService", fact.resolvedTypeName());
        assertEquals(32, fact.lineNumber());
        assertEquals(List.of(
                "org.springframework.beans.factory.annotation.Autowired",
                "org.springframework.beans.factory.annotation.Qualifier"), fact.annotations());
        assertEquals(List.of("org.springframework.stereotype.Controller"), fact.classAnnotations());
        assertEquals(List.of("demo.ControllerContract"), fact.implementedInterfaces());
        assertEquals(List.of("demo.BaseController"), fact.superClassPath());
        assertEquals(List.of("java.lang.String"), fact.parameterTypes());
        assertEquals("demo.UserController#query", fact.methodSignature());
        assertEquals("userService", fact.annotationAttributes().get("requestedName"));
        assertTrue(fact.qualifierPresent());
    }
}
