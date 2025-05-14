package com.puti.code.repository.graph.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地图节点记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileGraphNodeRecord {

    private String id;
    private String nodeType;
    private String tag;
    private String fullName;
    @Builder.Default
    private Map<String, Object> properties = new LinkedHashMap<>();
}
