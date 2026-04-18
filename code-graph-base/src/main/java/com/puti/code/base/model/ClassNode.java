package com.puti.code.base.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 类节点
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ClassNode extends Node {
    private String name;
    private String fullName;
    private String type;
    private String visibility;
    private Boolean isLibrary;
    private Integer lineStart;
    private Integer lineEnd;
    private String branchName;
    private String commitStatus;
    private String commitId;
    private LocalDateTime lastUpdated;
    private String repoId;
    private Boolean isExternal;
    private String filePath;

    @Override
    public String getTag() {
        return NodeType.CLASS.getValue();
    }

    @Override
    public Object[] getProperties() {
        return new Object[]{
                name, fullName, type, visibility, isLibrary, lineStart, lineEnd,
                branchName, commitStatus, commitId, lastUpdated, repoId,
                isExternal, filePath, getContent()
        };
    }

    @Override
    public String[] getPropertyNames() {
        return new String[]{
                "name", "full_name", "type", "visibility", "is_library", "line_start", "line_end",
                "branch_name", "commit_status", "commit_id", "last_updated", "repo_id",
                "is_external", "file_path", "content"
        };
    }
}
