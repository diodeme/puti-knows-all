package com.puti.code.base.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 运行时注册 edge 类型的轻量封装。
 */
@Getter
@RequiredArgsConstructor
public class RegisteredEdgeType implements EdgeDefinition {
    private final EdgeSchema schema;

    @Override
    public String getValue() {
        return schema.getValue();
    }

    @Override
    public EdgeCategory getCategory() {
        return schema.getCategory();
    }

    @Override
    public String getDisplayName() {
        return schema.getDisplayName();
    }

    @Override
    public List<EdgePropertySchema> getPropertySchemas() {
        return schema.getPropertySchemas();
    }
}
