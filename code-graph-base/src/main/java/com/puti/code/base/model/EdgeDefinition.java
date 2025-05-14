package com.puti.code.base.model;

import java.util.List;

/**
 * 统一的 edge 定义接口，既支持内建边，也支持运行时注册边。
 */
public interface EdgeDefinition {

    String getValue();

    EdgeCategory getCategory();

    List<EdgePropertySchema> getPropertySchemas();

    default String getDisplayName() {
        return getValue();
    }
}
