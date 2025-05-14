package com.puti.code.analyzer.java.processor;

import com.puti.code.rule.config.RuleConfiguration;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInjectionResolutionSupportTest {

    @Test
    void shouldResolveQualifierBeforeOtherFallbacks() {
        DependencyInjectionResolutionSupport support = new DependencyInjectionResolutionSupport();
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("com.demo.UserService")
                .actualType("com.demo.FastUserService")
                .providerName("fastUserService")
                .qualifiers(List.of("fastUserService"))
                .priority(0)
                .build());
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("com.demo.UserService")
                .actualType("com.demo.SafeUserService")
                .providerName("safeUserService")
                .qualifiers(List.of("safeUserService"))
                .priority(100)
                .build());

        DependencyInjectionResolution resolution = support.resolve(InjectionPoint.builder()
                .ownerTypeName("com.demo.UserController")
                .memberName("userService")
                .memberKind("field")
                .requiredType("com.demo.UserService")
                .requestedName("fastUserService")
                .qualifiers(List.of("fastUserService"))
                .injectionMode("autowired")
                .build());

        assertTrue(resolution.isCandidateMatched());
        assertEquals("com.demo.FastUserService", resolution.getResolvedTypeName());
        assertEquals("qualifier", resolution.getResolutionKind());
    }

    @Test
    void shouldFallbackToPriorityCandidateWhenNoQualifierMatches() {
        DependencyInjectionResolutionSupport support = new DependencyInjectionResolutionSupport();
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("com.demo.UserService")
                .actualType("com.demo.FastUserService")
                .providerName("fastUserService")
                .priority(0)
                .build());
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("com.demo.UserService")
                .actualType("com.demo.SafeUserService")
                .providerName("safeUserService")
                .priority(100)
                .build());

        DependencyInjectionResolution resolution = support.resolve(InjectionPoint.builder()
                .ownerTypeName("com.demo.UserController")
                .memberName("userService")
                .memberKind("field")
                .requiredType("com.demo.UserService")
                .injectionMode("autowired")
                .build());

        assertTrue(resolution.isCandidateMatched());
        assertEquals("com.demo.SafeUserService", resolution.getResolvedTypeName());
        assertEquals("priority", resolution.getResolutionKind());
        assertTrue(resolution.isPriorityCandidateMatched());
    }

    @Test
    void shouldRespectConfiguredResolutionOrder() {
        RuleConfiguration.DependencyInjectionResolutionRules resolutionRules =
                new RuleConfiguration.DependencyInjectionResolutionRules();
        resolutionRules.setOrder(List.of("priority"));
        RuleConfiguration.DependencyInjectionRules rules = new RuleConfiguration.DependencyInjectionRules();
        rules.setResolutionRules(resolutionRules);

        DependencyInjectionResolutionSupport support = new DependencyInjectionResolutionSupport(rules);
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("com.demo.UserService")
                .actualType("com.demo.NamedUserService")
                .providerName("userService")
                .priority(0)
                .build());
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("com.demo.UserService")
                .actualType("com.demo.PriorityUserService")
                .providerName("priorityUserService")
                .priority(100)
                .build());

        DependencyInjectionResolution resolution = support.resolve(InjectionPoint.builder()
                .ownerTypeName("com.demo.UserController")
                .memberName("userService")
                .memberKind("field")
                .requiredType("com.demo.UserService")
                .injectionMode("autowired")
                .build());

        assertTrue(resolution.isCandidateMatched());
        assertEquals("com.demo.PriorityUserService", resolution.getResolvedTypeName());
        assertEquals("priority", resolution.getResolutionKind());
    }

    @Test
    void shouldRecognizeManagedAnnotationsFromFactMapping() {
        RuleConfiguration.DependencyInjectionFactMapping factMapping = new RuleConfiguration.DependencyInjectionFactMapping();
        factMapping.setProviderAnnotations(List.of(
                providerAnnotation("demo.framework.Provider", "TYPE"),
                providerAnnotation("demo.framework.Factory", "METHOD")));
        factMapping.setInjectionAnnotations(List.of(
                injectionAnnotation("demo.framework.Inject", "inject")));
        factMapping.setQualifierAnnotations(List.of(
                qualifierAnnotation("demo.framework.Named")));
        factMapping.setPriorityAnnotations(List.of(
                priorityAnnotation("demo.framework.Primary", 10)));

        RuleConfiguration.DependencyInjectionRules rules = new RuleConfiguration.DependencyInjectionRules();
        rules.setFactMapping(factMapping);
        DependencyInjectionResolutionSupport support = new DependencyInjectionResolutionSupport(rules);

        assertTrue(support.isTypeProviderAnnotation("demo.framework.Provider"));
        assertTrue(support.isMethodProviderAnnotation("demo.framework.Factory"));
        assertTrue(support.isInjectionOrQualifierAnnotation("demo.framework.Inject"));
        assertTrue(support.isInjectionOrQualifierAnnotation("demo.framework.Named"));
        assertTrue(support.isManagedFrameworkAnnotation("demo.framework.Primary"));
    }

    @Test
    void shouldExposeSuperclassTypesWhenRegisteringProvider() {
        DependencyInjectionResolutionSupport support = new DependencyInjectionResolutionSupport();
        CtType<?> userServiceType = buildType("demo.UserService",
                new VirtualFile("""
                        package org.springframework.stereotype;
                        public @interface Service {
                            String value() default "";
                        }
                        """, "org/springframework/stereotype/Service.java"),
                new VirtualFile("""
                        package demo;
                        public interface IUserService {
                        }
                        """, "demo/IUserService.java"),
                new VirtualFile("""
                        package demo;
                        public abstract class BaseUserService implements IUserService {
                        }
                        """, "demo/BaseUserService.java"),
                new VirtualFile("""
                        package demo;
                        import org.springframework.stereotype.Service;

                        @Service
                        public class UserService extends BaseUserService {
                        }
                        """, "demo/UserService.java"));
        CtAnnotation<?> serviceAnnotation = userServiceType.getAnnotations().getFirst();
        support.registerTypeProvider(userServiceType, serviceAnnotation, Map.of());

        DependencyInjectionResolution resolution = support.resolve(InjectionPoint.builder()
                .ownerTypeName("demo.UserController")
                .memberName("userService")
                .memberKind("field")
                .requiredType("demo.BaseUserService")
                .injectionMode("autowired")
                .build());

        assertTrue(resolution.isCandidateMatched());
        assertEquals("demo.UserService", resolution.getResolvedTypeName());
        assertTrue(support.snapshotTypeRelations().stream().anyMatch(relation ->
                "demo.UserService".equals(relation.getSourceType())
                        && "demo.BaseUserService".equals(relation.getTargetType())
                        && "extends".equals(relation.getRelationKind())));
        assertTrue(support.snapshotTypeRelations().stream().anyMatch(relation ->
                "demo.BaseUserService".equals(relation.getSourceType())
                        && "demo.IUserService".equals(relation.getTargetType())
                        && "implements".equals(relation.getRelationKind())));
    }

    @Test
    void shouldResolveInheritedFieldInjectionFromSubclass() {
        DependencyInjectionResolutionSupport support = new DependencyInjectionResolutionSupport();
        support.registerProviderCandidate(ProviderCandidate.builder()
                .declaredType("demo.IUserService")
                .actualType("demo.UserService")
                .providerName("userService")
                .qualifiers(List.of("userService"))
                .priority(0)
                .build());
        CtType<?> userControllerType = buildType("demo.UserController",
                new VirtualFile("""
                        package org.springframework.beans.factory.annotation;
                        public @interface Autowired {
                        }
                        """, "org/springframework/beans/factory/annotation/Autowired.java"),
                new VirtualFile("""
                        package demo;
                        public interface IUserService {
                        }
                        """, "demo/IUserService.java"),
                new VirtualFile("""
                        package demo;
                        import org.springframework.beans.factory.annotation.Autowired;

                        public class BaseController {
                            @Autowired
                            protected IUserService userService;
                        }
                        """, "demo/BaseController.java"),
                new VirtualFile("""
                        package demo;
                        public class UserController extends BaseController {
                        }
                        """, "demo/UserController.java"));

        ResolvedInjectionPoint resolvedInjectionPoint = support.resolveFieldInjection(userControllerType, "userService")
                .orElse(null);

        assertNotNull(resolvedInjectionPoint);
        assertEquals("demo.IUserService", resolvedInjectionPoint.injectionPoint().getRequiredType());
        assertTrue(resolvedInjectionPoint.resolution().isCandidateMatched());
        assertEquals("demo.UserService", resolvedInjectionPoint.resolution().getResolvedTypeName());
    }

    private RuleConfiguration.ProviderAnnotationMapping providerAnnotation(String annotation, String target) {
        RuleConfiguration.ProviderAnnotationMapping mapping = new RuleConfiguration.ProviderAnnotationMapping();
        mapping.setAnnotation(annotation);
        mapping.setTarget(target);
        return mapping;
    }

    private RuleConfiguration.InjectionAnnotationMapping injectionAnnotation(String annotation, String mode) {
        RuleConfiguration.InjectionAnnotationMapping mapping = new RuleConfiguration.InjectionAnnotationMapping();
        mapping.setAnnotation(annotation);
        mapping.setMode(mode);
        return mapping;
    }

    private RuleConfiguration.QualifierAnnotationMapping qualifierAnnotation(String annotation) {
        RuleConfiguration.QualifierAnnotationMapping mapping = new RuleConfiguration.QualifierAnnotationMapping();
        mapping.setAnnotation(annotation);
        return mapping;
    }

    private RuleConfiguration.PriorityAnnotationMapping priorityAnnotation(String annotation, Integer priority) {
        RuleConfiguration.PriorityAnnotationMapping mapping = new RuleConfiguration.PriorityAnnotationMapping();
        mapping.setAnnotation(annotation);
        mapping.setPriority(priority);
        return mapping;
    }

    private CtType<?> buildType(String qualifiedName, VirtualFile... sourceFiles) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        for (VirtualFile sourceFile : sourceFiles) {
            launcher.addInputResource(sourceFile);
        }
        launcher.buildModel();
        return launcher.getFactory().Type().get(qualifiedName);
    }
}
