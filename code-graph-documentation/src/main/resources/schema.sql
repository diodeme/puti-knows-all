-- 说明书表
CREATE TABLE IF NOT EXISTS documentation
(
    id                      VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    entry_point_id          VARCHAR(255) NOT NULL COMMENT '入口节点ID',
    entry_point_name        VARCHAR(500) COMMENT '入口节点名称',
    title                   VARCHAR(500) COMMENT '说明书标题',
    summary                 TEXT COMMENT '说明书摘要',
    content                 MEDIUMTEXT COMMENT '说明书内容',
    level                   INT          NOT NULL COMMENT '生成层级（1-3）',
    status                  VARCHAR(50)  NOT NULL COMMENT '生成状态',
    version                 INT        DEFAULT 1 COMMENT '版本号',
    is_final_version        TINYINT(1) DEFAULT 0 COMMENT '是否为最终版本',
    parent_documentation_id VARCHAR(32) COMMENT '父文档ID',
    project_id              VARCHAR(100) COMMENT '项目ID',
    branch_name             VARCHAR(100) COMMENT '分支名称',
    created_at              TIMESTAMP  DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              TIMESTAMP  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_entry_point_id (entry_point_id),
    INDEX idx_entry_point_level (entry_point_id, level),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_is_final_version (is_final_version),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='说明书表';

-- 说明书方法信息表
CREATE TABLE IF NOT EXISTS documentation_method
(
    id               VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    documentation_id VARCHAR(32) NOT NULL COMMENT '说明书ID',
    method_id        VARCHAR(255) NOT NULL COMMENT '方法节点ID',
    method_name      VARCHAR(500) NOT NULL COMMENT '方法全限定名',
    method_type      VARCHAR(50)  NOT NULL COMMENT '方法类型',
    call_level       INT COMMENT '调用层级',
    description      TEXT COMMENT '方法描述',
    signature        VARCHAR(1000) COMMENT '方法签名',
    class_name       VARCHAR(500) COMMENT '所属类名',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_documentation_id (documentation_id),
    INDEX idx_method_id (method_id),
    INDEX idx_call_level (call_level),
    INDEX idx_method_type (method_type),
    INDEX idx_class_name (class_name),

    FOREIGN KEY (documentation_id) REFERENCES documentation (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='说明书方法信息表';

-- 说明书生成任务表
CREATE TABLE IF NOT EXISTS documentation_task
(
    id               VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    entry_point_id   VARCHAR(255)                                                    NOT NULL COMMENT '入口节点ID',
    entry_point_name VARCHAR(500) COMMENT '入口节点名称',
    target_level     INT                                                             NOT NULL COMMENT '目标生成层级',
    current_level    INT                                                                      DEFAULT 0 COMMENT '当前处理层级',
    status           ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    progress         INT                                                                      DEFAULT 0 COMMENT '进度百分比',
    error_message    TEXT COMMENT '错误信息',
    project_id       VARCHAR(100) COMMENT '项目ID',
    branch_name      VARCHAR(100) COMMENT '分支名称',
    created_at       TIMESTAMP                                                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP                                                                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    completed_at     TIMESTAMP                                                       NULL COMMENT '完成时间',

    INDEX idx_entry_point_id (entry_point_id),
    INDEX idx_status (status),
    INDEX idx_project_id (project_id),
    INDEX idx_created_at (created_at),
    INDEX idx_completed_at (completed_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='说明书生成任务表';

-- 说明书归档表
CREATE TABLE IF NOT EXISTS documentation_archive
(
    id                        VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    original_documentation_id VARCHAR(32) NOT NULL COMMENT '原始文档ID',
    entry_point_id            VARCHAR(255) NOT NULL COMMENT '入口节点ID',
    entry_point_name          VARCHAR(500) COMMENT '入口节点名称',
    title                     VARCHAR(500) COMMENT '说明书标题',
    summary                   TEXT COMMENT '说明书摘要',
    content                   MEDIUMTEXT COMMENT '说明书内容',
    level                     INT          NOT NULL COMMENT '生成层级',
    version                   INT COMMENT '版本号',
    project_id                VARCHAR(100) COMMENT '项目ID',
    branch_name               VARCHAR(100) COMMENT '分支名称',
    original_created_at       TIMESTAMP    NULL DEFAULT NULL COMMENT '原始创建时间',
    original_updated_at       TIMESTAMP    NULL DEFAULT NULL COMMENT '原始更新时间',

    archived_at               TIMESTAMP         DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    archive_reason            VARCHAR(100) COMMENT '归档原因',
    final_documentation_id    VARCHAR(32) COMMENT '最终版本文档ID',

    INDEX idx_original_documentation_id (original_documentation_id),
    INDEX idx_entry_point_id (entry_point_id),
    INDEX idx_archived_at (archived_at),
    INDEX idx_archive_reason (archive_reason)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='说明书归档表';

-- 聚合说明书表
CREATE TABLE IF NOT EXISTS aggregated_documentation
(
    id               VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    title            VARCHAR(500) COMMENT '聚合标题',
    summary          TEXT COMMENT '聚合摘要',
    content          MEDIUMTEXT COMMENT '聚合内容',
    project_id       VARCHAR(100) COMMENT '项目ID',
    branch_name      VARCHAR(100) COMMENT '分支名称',
    aggregation_type VARCHAR(100) COMMENT '聚合类型',
    status           VARCHAR(50)  NOT NULL COMMENT '聚合状态',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_project_branch (project_id, branch_name),
    INDEX idx_aggregation_type (aggregation_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聚合说明书表';

-- 说明书聚合关系表
CREATE TABLE IF NOT EXISTS documentation_aggregation
(
    id                           VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    aggregated_documentation_id  VARCHAR(32) NOT NULL COMMENT '聚合说明书ID',
    documentation_id             VARCHAR(32) NOT NULL COMMENT '原始说明书ID',
    entry_point_id               VARCHAR(255) NOT NULL COMMENT '入口点ID',
    entry_point_name             VARCHAR(500) COMMENT '入口点名称',
    weight                       INT DEFAULT 0 COMMENT '在聚合中的权重',
    created_at                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_aggregated_documentation_id (aggregated_documentation_id),
    INDEX idx_documentation_id (documentation_id),
    INDEX idx_entry_point_id (entry_point_id),
    INDEX idx_weight (weight),

    FOREIGN KEY (aggregated_documentation_id) REFERENCES aggregated_documentation (id) ON DELETE CASCADE,
    FOREIGN KEY (documentation_id) REFERENCES documentation (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='说明书聚合关系表';

-- 说明书方法归档表
CREATE TABLE IF NOT EXISTS documentation_method_archive
(
    id                        VARCHAR(32) PRIMARY KEY COMMENT '分布式ID',
    original_method_id        VARCHAR(32) NOT NULL COMMENT '原始方法记录ID',
    archived_documentation_id VARCHAR(32) NOT NULL COMMENT '归档文档ID',
    method_id                 VARCHAR(255) NOT NULL COMMENT '方法节点ID',
    method_name               VARCHAR(500) NOT NULL COMMENT '方法全限定名',
    method_type               VARCHAR(50)  NOT NULL COMMENT '方法类型',
    call_level                INT COMMENT '调用层级',
    description               TEXT COMMENT '方法描述',
    signature                 VARCHAR(1000) COMMENT '方法签名',
    class_name                VARCHAR(500) COMMENT '所属类名',
    original_created_at       TIMESTAMP NULL DEFAULT NULL COMMENT '原始创建时间',
    archived_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',

    INDEX idx_archived_documentation_id (archived_documentation_id),
    INDEX idx_method_id (method_id),
    INDEX idx_archived_at (archived_at),

    FOREIGN KEY (archived_documentation_id) REFERENCES documentation_archive (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='说明书方法归档表';
