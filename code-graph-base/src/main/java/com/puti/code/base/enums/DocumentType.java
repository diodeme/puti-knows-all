package com.puti.code.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DocumentType {

    DOC_TYPE_FLOW(999,"document-flow"),
    DOC_TYPE_CHAIN_1(1,"document-chain-1"),
    DOC_TYPE_CHAIN_2(2,"document-chain-2"),
    DOC_TYPE_CHAIN_3(3,"document-chain-3"),
    ;

    @Getter
    private final Integer code;
    @Getter
    private final String value;

    public static DocumentType getByLevel(Integer level) {
        return switch (level) {
            case 1 -> DOC_TYPE_CHAIN_1;
            case 2 -> DOC_TYPE_CHAIN_2;
            default -> DOC_TYPE_CHAIN_3;
        };
    }
}
