package com.puti.code.base.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 文件节点
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FileNode extends Node {
    private String filePath;
    private String name;
    private String extension;
    private Boolean isLibrary;
    private LocalDateTime lastModified;
    private String language;
    private String branchName;
    private String commitStatus;
    private String commitId;
    private LocalDateTime lastUpdated;
    private String repoId;

    @Override
    public String getTag() {
        return NodeType.FILE.getValue();
    }

    @Override
    public Object[] getProperties() {
        return new Object[]{
                filePath, name, extension, isLibrary, lastModified, language,
                branchName, commitStatus, commitId, lastUpdated, repoId
        };
    }

    @Override
    public String[] getPropertyNames() {
        return new String[]{
                "file_path", "name", "extension", "is_library", "last_modified", "language",
                "branch_name", "commit_status", "commit_id", "last_updated", "repo_id"
        };
    }
}
