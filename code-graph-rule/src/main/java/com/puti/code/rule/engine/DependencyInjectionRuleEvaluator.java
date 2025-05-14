package com.puti.code.rule.engine;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.rule.config.RuleConfiguration;
import com.puti.code.rule.context.RuleContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于规则的依赖注入增强评估器。
 */
@Slf4j
public class DependencyInjectionRuleEvaluator {

    private final RuleConfiguration configuration = RuleConfiguration.getInstance();
    private final GenericMutationRuleEvaluator mutationRuleEvaluator =
            new GenericMutationRuleEvaluator(new SpelRuleEvaluationSupport());

    public RuleMatchResult evaluate(RuleContext context) {
        RuleConfiguration.DependencyInjectionRules rules = configuration.getDependencyInjectionRules();
        if (rules == null || !Boolean.TRUE.equals(rules.getEnabled()) || rules.getRules() == null || rules.getRules().isEmpty()) {
            return RuleMatchResult.builder().matched(false).build();
        }
        return mutationRuleEvaluator.evaluate(rules.getRules(), context, rules.getCustomFunctions());
    }
}
