package com.puti.code.rule.factory;

import com.puti.code.base.enums.RuleEngineType;
import com.puti.code.rule.engine.RuleEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则引擎工厂
 * 支持外部注册规则引擎，不依赖具体的实现
 */
@Slf4j
public class RuleEngineFactory {

    private static final RuleEngineFactory INSTANCE = new RuleEngineFactory();
    private final Map<RuleEngineType, RuleEngine<?, ?>> engines = new HashMap<>();

    private RuleEngineFactory() {
        // 不再自动初始化，由外部模块注册
    }

    public static RuleEngineFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 注册规则引擎
     *
     * @param ruleEngine 规则引擎实例
     */
    public void registerEngine(RuleEngine<?, ?> ruleEngine) {
        engines.put(ruleEngine.getType(), ruleEngine);
        log.info("Registered rule engine: {}", ruleEngine.getType());
    }
    
    /**
     * 获取规则引擎
     * 
     * @param type 规则引擎类型
     * @param <T> 目标对象类型
     * @param <R> 结果类型
     * @return 规则引擎
     */
    @SuppressWarnings("unchecked")
    public <T, R> RuleEngine<T, R> getEngine(RuleEngineType type) {
        RuleEngine<?, ?> engine = engines.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("Unknown rule engine type: " + type);
        }
        return (RuleEngine<T, R>) engine;
    }

    /**
     * 检查是否已注册指定类型的规则引擎
     *
     * @param type 规则引擎类型
     * @return 是否已注册
     */
    public boolean hasEngine(RuleEngineType type) {
        return engines.containsKey(type);
    }
}
