package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CodeSearchData {

    private String query;
    private List<SearchResult> results;
    private SearchMeta meta;

    @Data
    public static class SearchResult {
        private String id;

        @JsonProperty("node_type")
        private String nodeType;

        @JsonProperty("full_name")
        private String fullName;

        private String name;
        private String digest;
        private String content;
        private Float score;

        @JsonProperty("is_library")
        private Boolean isLibrary;

        @JsonProperty("source_code_location")
        private String sourceCodeLocation;

        private SearchContext context;
    }

    @Data
    public static class SearchContext {
        private List<ContextNode> upstream;
        private List<ContextNode> downstream;
    }

    @Data
    public static class ContextNode {
        private String id;

        @JsonProperty("node_type")
        private String nodeType;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("edge_type")
        private String edgeType;

        @JsonProperty("edge_properties")
        private Map<String, Object> edgeProperties;
    }

    @Data
    public static class SearchMeta {
        @JsonProperty("total_vector_hits")
        private Integer totalVectorHits;

        private Integer returned;

        @JsonProperty("type_distribution")
        private Map<String, Integer> typeDistribution;

        @JsonProperty("strategy_used")
        private String strategyUsed;

        @JsonProperty("context_nodes_count")
        private Integer contextNodesCount;

        @JsonProperty("elapsed_ms")
        private Long elapsedMs;
    }
}
