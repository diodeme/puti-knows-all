package com.puti.code.repository.graph.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地图边记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileGraphEdgeRecord {

    private String source;
    private String target;
    private String type;
    private String category;
    @Builder.Default
    private Map<String, Object> properties = new LinkedHashMap<>();
}
