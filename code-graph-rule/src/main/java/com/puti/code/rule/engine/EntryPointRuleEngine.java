package com.puti.code.rule.engine;

import com.puti.code.rule.config.RuleConfiguration;
import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.function.RuleFunctions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;

/**
 * 入口点规则引擎抽象基类
 * 提供通用的规则执行逻辑，具体的目标对象类型由子类定义
 */
@Slf4j
public abstract class EntryPointRuleEngine<T> implements RuleEngine<T, Boolean> {
    
    private final ExpressionParser parser;
    private final RuleConfiguration ruleConfiguration;
    private final CustomFunctionRegistry customFunctionRegistry;
    
    public EntryPointRuleEngine() {
        this.parser = new SpelExpressionParser();
        this.ruleConfiguration = RuleConfiguration.getInstance();
        this.customFunctionRegistry = new CustomFunctionRegistry();
    }
    
    @Override
    public Boolean execute(T target, RuleContext context) {
        RuleConfiguration.EntryPointRules entryPointRules = ruleConfiguration.getEntryPointRules();

        // 如果规则未启用，返回false
        if (entryPointRules == null || !entryPointRules.getEnabled()) {
            return false;
        }

        List<String> rules = entryPointRules.getRules();
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        // 创建SpEL评估上下文
        StandardEvaluationContext evaluationContext = createEvaluationContext(context);

        // 执行所有规则，任意一个规则返回true则认为是入口点
        for (String rule : rules) {
            try {
                Expression expression = parser.parseExpression(rule);
                Boolean result = expression.getValue(evaluationContext, Boolean.class);

                if (Boolean.TRUE.equals(result)) {
                    log.debug("Target {} matched entry point rule: {}",
                            getTargetSignature(target), rule);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate rule: {} for target: {}",
                        rule, getTargetSignature(target), e);
            }
        }

        return false;
    }

    /**
     * 获取目标对象的签名，用于日志输出
     *
     * @param target 目标对象
     * @return 签名字符串
     */
    protected abstract String getTargetSignature(T target);
    
    /**
     * 创建SpEL评估上下文
     *
     * @param context 规则上下文
     * @return SpEL评估上下文
     */
    protected StandardEvaluationContext createEvaluationContext(RuleContext context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

        // 注册自定义函数对象
        RuleFunctions functions = createRuleFunctions(context);
        evaluationContext.setRootObject(functions);

        // 注册变量
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

        // 注册自定义函数（如果有配置）
        RuleConfiguration.EntryPointRules entryPointRules = ruleConfiguration.getEntryPointRules();
        customFunctionRegistry.register(entryPointRules.getCustomFunctions(), evaluationContext, parser);

        return evaluationContext;
    }

    /**
     * 创建规则函数对象
     * 子类可以重写此方法来提供特定的函数实现
     *
     * @param context 规则上下文
     * @return 规则函数对象
     */
    protected RuleFunctions createRuleFunctions(RuleContext context) {
        return new RuleFunctions(context);
    }
}
