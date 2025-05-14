package com.puti.code.rule.engine;

import com.puti.code.base.enums.RuleEngineType;
import com.puti.code.rule.context.RuleContext;

/**
 * 规则引擎接口
 * 
 * @param <T> 规则执行的目标对象类型
 * @param <R> 规则执行的结果类型
 */
public interface RuleEngine<T, R> {
    
    /**
     * 执行规则
     * 
     * @param target 目标对象
     * @param context 规则上下文
     * @return 规则执行结果
     */
    R execute(T target, RuleContext context);
    
    /**
     * 获取规则引擎类型
     * 
     * @return 规则引擎类型
     */
    RuleEngineType getType();
}
