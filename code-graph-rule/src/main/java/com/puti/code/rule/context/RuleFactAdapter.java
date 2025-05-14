package com.puti.code.rule.context;

/**
 * 将特定语义事实适配成统一规则上下文。
 *
 * @param <T> 事实对象类型
 */
public interface RuleFactAdapter<T> {

    RuleContext adapt(T fact);
}
