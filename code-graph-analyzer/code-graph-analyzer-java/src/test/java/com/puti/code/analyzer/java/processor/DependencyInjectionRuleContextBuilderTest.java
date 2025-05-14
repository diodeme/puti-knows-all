package com.puti.code.analyzer.java.processor;

import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.context.RuleContextKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInjectionRuleContextBuilderTest {

    private final DependencyInjectionRuleContextBuilder builder = new DependencyInjectionRuleContextBuilder();

    @Test
    void shouldBuildRuleContextFromFact() {
        DependencyInjectionFact fact = DependencyInjectionFact.builder()
                .factKind(DependencyInjectionFactKinds.BINDING)
                .ownerTypeName("com.demo.UserService")
                .memberName("userRepository")
                .memberKind("field")
                .requiredTypeName("com.demo.UserRepository")
                .declaredTargetTypeName("com.demo.UserRepository")
                .requestedName("userRepository")
                .qualifierPresent(true)
                .candidateMatched(true)
                .requestedNamePresent(true)
                .priorityCandidateMatched(false)
                .uniqueCandidateMatched(false)
                .lineNumber(12)
                .annotations(java.util.List.of("org.springframework.beans.factory.annotation.Autowired"))
                .annotationAttributes(java.util.Map.of("requestedName", "userRepository"))
                .classAnnotations(java.util.List.of("org.springframework.stereotype.Service"))
                .implementedInterfaces(java.util.List.of("com.demo.RepositoryAware"))
                .superClassPath(java.util.List.of("com.demo.BaseService"))
                .methodSignature("com.demo.UserService#findUser")
                .parameterTypes(java.util.List.of("java.lang.String"))
                .returnTypeName("com.demo.UserRepository")
                .build();

        RuleContext context = builder.adapt(fact);

        assertEquals("com.demo.UserService", context.getOwnerTypeName());
        assertEquals("findUser", context.getMethodName());
        assertEquals("com.demo.UserService#findUser", context.getMethodSignature());
        assertEquals("com.demo.UserRepository", context.getReturnTypeName());
        assertEquals(java.util.List.of("org.springframework.beans.factory.annotation.Autowired"), context.getAnnotations());
        assertEquals(java.util.Map.of("requestedName", "userRepository"), context.getAnnotationAttributes());
        assertEquals(java.util.List.of("org.springframework.stereotype.Service"), context.getClassAnnotations());
        assertEquals(java.util.List.of("com.demo.RepositoryAware"), context.getImplementedInterfaces());
        assertEquals(java.util.List.of("com.demo.BaseService"), context.getSuperClassPath());
        assertEquals(java.util.List.of("java.lang.String"), context.getParameterTypes());
        assertEquals(DependencyInjectionFactKinds.BINDING, context.getAdditionalData(RuleContextKeys.FACT_KIND, ""));
        assertEquals("userRepository", context.getAdditionalData(RuleContextKeys.MEMBER_NAME, ""));
        assertEquals("field", context.getAdditionalData(RuleContextKeys.MEMBER_KIND, ""));
        assertEquals("com.demo.UserRepository", context.getAdditionalData(RuleContextKeys.REQUIRED_TYPE_NAME, ""));
        assertEquals("com.demo.UserRepository",
                context.getAdditionalData(RuleContextKeys.DECLARED_TARGET_TYPE_NAME, ""));
        assertTrue(context.getAdditionalData(RuleContextKeys.REQUESTED_NAME_PRESENT, false));
        assertTrue(context.getAdditionalData(RuleContextKeys.QUALIFIER_PRESENT, false));
    }
}
