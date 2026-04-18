package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EntryPointsData {

    @JsonProperty("entry_points")
    private List<EntryPoint> entryPoints;

    private EntryPointsMeta meta;

    @Data
    public static class EntryPoint {
        private String id;

        @JsonProperty("node_type")
        private String nodeType;

        @JsonProperty("full_name")
        private String fullName;

        private String name;

        @JsonProperty("source_code_location")
        private String sourceCodeLocation;
    }

    @Data
    public static class EntryPointsMeta {
        private Integer total;
        private Integer returned;

        @JsonProperty("elapsed_ms")
        private Long elapsedMs;
    }
}
