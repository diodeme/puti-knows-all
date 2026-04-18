package com.puti.code.base.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 注解节点
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AnnotationNode extends Node {
    private String name;
    private String fullName;
    private String type;
    private Integer lineStart;
    private Integer lineEnd;
    private String branchName;
    private String commitStatus;
    private String commitId;
    private LocalDateTime lastUpdated;
    private String repoId;
    private String filePath;

    @Override
    public String getTag() {
        return NodeType.ANNOTATION.getValue();
    }

    @Override
    public Object[] getProperties() {
        return new Object[]{
                name, fullName, type, lineStart, lineEnd, branchName,
                commitStatus, commitId, lastUpdated, repoId, filePath, getContent()
        };
    }

    @Override
    public String[] getPropertyNames() {
        return new String[]{
                "name", "full_name", "type", "line_start", "line_end", "branch_name",
                "commit_status", "commit_id", "last_updated", "repo_id", "file_path", "content"
        };
    }
}
