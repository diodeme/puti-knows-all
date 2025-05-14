package com.puti.code.analyzer.java.processor;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 通用调用点事实。
 */
@Value
@Builder
public class CallSite {
    String callerId;
    String receiverSymbol;
    String declaredReceiverType;
    String methodName;
    @Builder.Default
    List<String> argumentTypes = List.of();
    String location;
    Integer lineNumber;
}
