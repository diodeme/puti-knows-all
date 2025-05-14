package com.puti.code.analyzer.java.rule;

import com.puti.code.base.enums.RuleEngineType;
import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.engine.EntryPointRuleEngine;
import com.puti.code.rule.function.RuleFunctions;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtExecutable;

/**
 * Spoon入口点规则引擎实现
 * 专门处理Spoon CtExecutable对象的入口点判断
 */
@Slf4j
public class SpoonEntryPointRuleEngine extends EntryPointRuleEngine<CtExecutable<?>> {

    @Override
    protected String getTargetSignature(CtExecutable<?> target) {
        return target != null ? target.getSignature() : "unknown";
    }
    
    @Override
    protected RuleFunctions createRuleFunctions(RuleContext context) {
        // 可以在这里返回Spoon特定的函数实现
        // 目前使用基础实现就足够了
        return new RuleFunctions(context);
    }

    @Override
    public RuleEngineType getType() {
        return RuleEngineType.JAVA_ENTRY_POINT;
    }
}
