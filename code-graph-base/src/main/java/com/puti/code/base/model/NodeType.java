package com.puti.code.base.model;

import lombok.Getter;

import java.util.Map;

import static java.util.Map.entry;

/**
 * 节点类型枚举
 */
@Getter
public enum NodeType {
    FILE("file", Map.ofEntries(
            entry("file_path", "string"), entry("name", "string"), entry("extension", "string"),
            entry("is_library", "bool"), entry("last_modified", "int64"), entry("language", "string"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string")
    )),
    CLASS("class", Map.ofEntries(
            entry("name", "string"), entry("full_name", "string"), entry("type", "string"),
            entry("visibility", "string"), entry("is_library", "bool"),
            entry("line_start", "int64"), entry("line_end", "int64"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string"),
            entry("is_external", "bool"), entry("file_path", "string"), entry("content", "string")
    )),
    FUNCTION("function", Map.ofEntries(
            entry("name", "string"), entry("full_name", "string"), entry("visibility", "string"),
            entry("is_static", "bool"), entry("is_constructor", "bool"), entry("is_library", "bool"),
            entry("is_entry_point", "bool"), entry("line_start", "int64"), entry("line_end", "int64"),
            entry("complexity", "int64"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string"), entry("file_path", "string"), entry("content", "string")
    )),
    COMMENT("comment", Map.ofEntries(
            entry("type", "string"),
            entry("line_start", "int64"), entry("line_end", "int64"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string"), entry("file_path", "string"), entry("content", "string")
    )),
    ANNOTATION("annotations", Map.ofEntries(
            entry("name", "string"), entry("full_name", "string"), entry("type", "string"),
            entry("line_start", "int64"), entry("line_end", "int64"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string"), entry("file_path", "string"), entry("content", "string")
    )),
    MARKER_ANNOTATION("marker_annotations", Map.ofEntries(
            entry("name", "string"), entry("full_name", "string"), entry("type", "string"),
            entry("line_start", "int64"), entry("line_end", "int64"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string"), entry("file_path", "string"), entry("content", "string")
    )),
    FIELD("field", Map.ofEntries(
            entry("name", "string"), entry("full_name", "string"), entry("type", "string"),
            entry("visibility", "string"), entry("is_static", "bool"),
            entry("line_start", "int64"), entry("line_end", "int64"),
            entry("branch_name", "string"), entry("commit_status", "string"), entry("commit_id", "string"),
            entry("last_updated", "int64"), entry("repo_id", "string"), entry("file_path", "string"), entry("content", "string")
    )),
    ;

    private final String value;
    private final Map<String, String> propertyDefinitions;

    NodeType(String value, Map<String, String> propertyDefinitions) {
        this.value = value;
        this.propertyDefinitions = propertyDefinitions;
    }

}
