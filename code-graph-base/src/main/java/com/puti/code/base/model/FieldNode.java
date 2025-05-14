package com.puti.code.base.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 字段节点
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FieldNode extends Node {
    private String name;
    private String fullName;
    private String type;
    private String visibility;
    private Boolean isStatic;
    private Integer lineStart;
    private Integer lineEnd;
    private String branchName;
    private String commitStatus;
    private String commitId;
    private LocalDateTime lastUpdated;
    private String repoId;

    @Override
    public String getTag() {
        return NodeType.FIELD.getValue();
    }

    @Override
    public Object[] getProperties() {
        return new Object[]{
                name, fullName, type, visibility, isStatic, lineStart, lineEnd,
                branchName, commitStatus, commitId, lastUpdated, repoId, getContent()
        };
    }

    @Override
    public String[] getPropertyNames() {
        return new String[]{
                "name", "full_name", "type", "visibility", "is_static", "line_start", "line_end",
                "branch_name", "commit_status", "commit_id", "last_updated", "repo_id", "content"
        };
    }
} 