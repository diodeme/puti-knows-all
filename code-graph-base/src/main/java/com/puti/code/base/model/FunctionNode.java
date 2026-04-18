package com.puti.code.base.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 函数/方法节点
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FunctionNode extends Node {
    private String name;
    private String fullName;
    private String visibility;
    private Boolean isStatic;
    private Boolean isConstructor;
    private Boolean isLibrary;
    private Boolean isEntryPoint;
    private Integer lineStart;
    private Integer lineEnd;
    private Integer complexity;
    private String branchName;
    private String commitStatus;
    private String commitId;
    private LocalDateTime lastUpdated;
    private String repoId;
    private String filePath;

    @Override
    public String getTag() {
        return NodeType.FUNCTION.getValue();
    }

    @Override
    public Object[] getProperties() {
        return new Object[]{
                name, fullName, visibility, isStatic, isConstructor, isLibrary, isEntryPoint, lineStart, lineEnd,
                complexity, branchName, commitStatus, commitId, lastUpdated, repoId, filePath, getContent()
        };
    }

    @Override
    public String[] getPropertyNames() {
        return new String[]{
                "name", "full_name", "visibility", "is_static", "is_constructor", "is_library", "is_entry_point", "line_start", "line_end",
                "complexity", "branch_name", "commit_status", "commit_id", "last_updated", "repo_id", "file_path", "content"
        };
    }
}