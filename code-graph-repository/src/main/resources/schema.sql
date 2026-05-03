-- 依赖跟踪数据库 Schema (MySQL)
-- 表名统一使用 puti_ 前缀 + _info 后缀

CREATE TABLE IF NOT EXISTS puti_project_info (
    project_id VARCHAR(128) NOT NULL,
    branch_name VARCHAR(64) NOT NULL,
    last_analyzed_commit VARCHAR(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, branch_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS puti_project_dependency_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(128) NOT NULL,
    branch_name VARCHAR(64) NOT NULL,
    gav VARCHAR(512) NOT NULL,
    jar_path VARCHAR(1024),
    scope VARCHAR(32) DEFAULT 'runtime',
    is_filtered TINYINT(1) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_proj_dep UNIQUE (project_id, branch_name, gav)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS puti_library_graph_info (
    gav VARCHAR(512) NOT NULL PRIMARY KEY,
    graph_status VARCHAR(32) DEFAULT 'PENDING',
    migration_status VARCHAR(32) DEFAULT 'NONE',
    migration_from_gav VARCHAR(512) DEFAULT NULL,
    node_count INT DEFAULT 0,
    edge_count INT DEFAULT 0,
    started_at TIMESTAMP DEFAULT NULL,
    built_at TIMESTAMP DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS puti_dependency_class_index (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gav VARCHAR(255) NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_dep_class UNIQUE (gav, class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_project_info_updated_at ON puti_project_info(updated_at);
CREATE INDEX idx_proj_dep_project ON puti_project_dependency_info(project_id, branch_name);
CREATE INDEX idx_proj_dep_gav ON puti_project_dependency_info(gav);
CREATE INDEX idx_lib_graph_status ON puti_library_graph_info(graph_status);
CREATE INDEX idx_lib_graph_built_at ON puti_library_graph_info(built_at);
CREATE INDEX idx_lib_graph_migration ON puti_library_graph_info(migration_status);
CREATE INDEX idx_dep_class_name ON puti_dependency_class_index(class_name);
