package com.puti.code.analyzer.java.processor;

/**
 * DI 事实类型常量，避免规则上下文到处散落字符串。
 */
public final class DependencyInjectionFactKinds {

    public static final String BINDING = "binding";
    public static final String CALL_RETARGET = "callRetarget";

    private DependencyInjectionFactKinds() {
    }
}
