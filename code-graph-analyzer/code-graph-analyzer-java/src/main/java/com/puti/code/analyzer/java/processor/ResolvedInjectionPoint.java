package com.puti.code.analyzer.java.processor;

/**
 * 注入点与解析结果的统一载体，供事实提取与调用重定向共享。
 *
 * @param injectionPoint 注入点事实
 * @param resolution 解析结果
 */
public record ResolvedInjectionPoint(
        InjectionPoint injectionPoint,
        DependencyInjectionResolution resolution) {
}
