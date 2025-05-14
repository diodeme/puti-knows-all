package com.puti.code.rule.engine;

import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.function.RuleFunctions;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * 构建基于 SpEL 的规则执行上下文。
 */
public class SpelRuleEvaluationSupport {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final CustomFunctionRegistry customFunctionRegistry = new CustomFunctionRegistry();

    public StandardEvaluationContext createEvaluationContext(RuleContext context) {
        return createEvaluationContext(context, Map.of());
    }

    public StandardEvaluationContext createEvaluationContext(RuleContext context,
                                                             Map<String, String> customFunctions) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        RuleFunctions functions = new RuleFunctions(context);
        evaluationContext.setRootObject(functions);
        evaluationContext.setVariable("methodName", functions.getMethodName());
        evaluationContext.setVariable("methodSignature", context.getMethodSignature());
        evaluationContext.setVariable("ownerTypeName", context.getOwnerTypeName());
        evaluationContext.setVariable("returnTypeName", context.getReturnTypeName());
        evaluationContext.setVariable("isPublic", functions.isPublic());
        evaluationContext.setVariable("isStatic", functions.isStatic());
        evaluationContext.setVariable("annotations", context.getAnnotations());
        evaluationContext.setVariable("implementedInterfaces", context.getImplementedInterfaces());
        evaluationContext.setVariable("classAnnotations", context.getClassAnnotations());
        evaluationContext.setVariable("superClass", context.getSuperClassPath());
        evaluationContext.setVariable("superClassPath", context.getSuperClassPath());
        evaluationContext.setVariable("annotationAttributes", context.getAnnotationAttributes());
        evaluationContext.setVariable("parameterTypes", context.getParameterTypes());
        if (context.getAdditionalData() != null) {
            context.getAdditionalData().forEach(evaluationContext::setVariable);
        }
        customFunctionRegistry.register(customFunctions, evaluationContext, parser);
        return evaluationContext;
    }
}
