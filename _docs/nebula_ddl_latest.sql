# Create Tag:
CREATE TAG `annotations` ( `name` string NOT NULL COMMENT "注解名", `full_name` string NOT NULL COMMENT "包含命名空间的完整注解名", `type` string NULL DEFAULT "ANNOTATION" COMMENT "注解类型, ANNOTATION, MARKER_ANNOTATION", `line_start` int64 NULL COMMENT "开始行号", `line_end` int64 NULL COMMENT "结束行号", `branch_name` string NULL COMMENT "所属分支名称", `commit_status` string NULL DEFAULT "UNCOMMITTED" COMMENT "提交状态: COMMITTED, UNCOMMITTED", `commit_id` string NULL COMMENT "若已提交，对应的提交ID", `last_updated` timestamp NULL COMMENT "最后更新时间", `repo_id` string NULL COMMENT "所属仓库ID", `content` string NULL COMMENT "内容") ttl_duration = 0, ttl_col = "", comment = "注解节点";
CREATE TAG `class` ( `name` string NOT NULL COMMENT "类名", `full_name` string NOT NULL COMMENT "包含命名空间的完整类名", `type` string NULL DEFAULT "class" COMMENT "类型: class, interface, enum, annotation", `visibility` string NULL DEFAULT "public" COMMENT "可见性（public/private/protected）", `line_start` int64 NULL COMMENT "开始行号", `line_end` int64 NULL COMMENT "结束行号", `branch_name` string NULL COMMENT "所属分支名称", `commit_status` string NULL DEFAULT "UNCOMMITTED" COMMENT "提交状态: COMMITTED, UNCOMMITTED", `commit_id` string NULL COMMENT "若已提交，对应的提交ID", `last_updated` timestamp NULL COMMENT "最后更新时间", `repo_id` string NULL COMMENT "所属仓库ID", `is_external` bool NULL DEFAULT false COMMENT "是否为外部文件", `content` string NULL COMMENT "内容", `is_library` bool NULL DEFAULT false COMMENT "是否为库文件") ttl_duration = 0, ttl_col = "", comment = "class节点";
CREATE TAG `comment` ( `type` string NULL DEFAULT "LINE" COMMENT "注释类型（LINE-行注释, BLOCK-块注释, DOC-文档注释", `line_start` int64 NULL COMMENT "开始行号", `line_end` int64 NULL COMMENT "结束行号", `branch_name` string NULL COMMENT "所属分支名称", `commit_status` string NULL DEFAULT "UNCOMMITTED" COMMENT "提交状态: COMMITTED, UNCOMMITTED", `commit_id` string NULL COMMENT "若已提交，对应的提交ID", `last_updated` timestamp NULL COMMENT "最后更新时间", `repo_id` string NULL COMMENT "所属仓库ID", `content` string NULL COMMENT "内容") ttl_duration = 0, ttl_col = "", comment = "注释节点";
CREATE TAG `field` ( `name` string NOT NULL COMMENT "字段名", `full_name` string NOT NULL COMMENT "包含命名空间的完整字段名", `type` string NULL DEFAULT "field" COMMENT "字段类型", `visibility` string NULL DEFAULT "public" COMMENT "可见性", `is_static` bool NULL DEFAULT false COMMENT "是否为静态字段", `line_start` int64 NULL COMMENT "开始行号", `line_end` int64 NULL COMMENT "结束行号", `branch_name` string NULL COMMENT "所属分支名称", `commit_status` string NULL DEFAULT "UNCOMMITTED" COMMENT "提交状态: COMMITTED, UNCOMMITTED", `commit_id` string NULL COMMENT "若已提交，对应的提交ID", `last_updated` timestamp NULL COMMENT "最后更新时间", `repo_id` string NULL COMMENT "所属仓库ID", `content` string NULL COMMENT "内容") ttl_duration = 0, ttl_col = "", comment = "字段节点";
CREATE TAG `file` ( `file_path` string NOT NULL COMMENT "文件路径", `name` string NOT NULL COMMENT "文件名", `extension` string NULL COMMENT "文件扩展名", `last_modified` timestamp NULL COMMENT "最后修改时间", `language` string NULL COMMENT "编程语言", `branch_name` string NULL COMMENT "所属分支名称", `commit_status` string NULL DEFAULT "UNCOMMITTED" COMMENT "提交状态: COMMITTED, UNCOMMITTED", `commit_id` string NULL COMMENT "若已提交，对应的提交ID", `last_updated` timestamp NULL COMMENT "最后更新时间", `repo_id` string NULL COMMENT "所属仓库ID", `is_library` bool NULL DEFAULT false COMMENT "是否为库文件") ttl_duration = 0, ttl_col = "", comment = "文件节点";
CREATE TAG `function` ( `name` string NOT NULL COMMENT "函数名", `full_name` string NOT NULL COMMENT "包含命名空间的完整函数名", `visibility` string NULL DEFAULT "public" COMMENT "可见性", `is_static` bool NULL DEFAULT false COMMENT "是否为静态方法", `is_constructor` bool NULL DEFAULT false COMMENT "是否为构造函数", `line_start` int64 NULL COMMENT "开始行号", `line_end` int64 NULL COMMENT "结束行号", `complexity` int64 NULL DEFAULT 1 COMMENT "圈复杂度", `branch_name` string NULL COMMENT "所属分支名称", `commit_status` string NULL DEFAULT "UNCOMMITTED" COMMENT "提交状态: COMMITTED, UNCOMMITTED", `commit_id` string NULL COMMENT "若已提交，对应的提交ID", `last_updated` timestamp NULL COMMENT "最后更新时间", `repo_id` string NULL COMMENT "所属仓库ID", `content` string NULL COMMENT "内容", `is_library` bool NULL DEFAULT false COMMENT "是否为库文件", `is_entry_point` bool NULL DEFAULT false COMMENT "是否为入口") ttl_duration = 0, ttl_col = "", comment = "函数/方法节点";

# Create Edge: 
CREATE EDGE `calls` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "调用关系";
CREATE EDGE `contains` () ttl_duration = 0, ttl_col = "", comment = "包含关系";
CREATE EDGE `depends_on` ( `dependency_type` string NULL COMMENT "依赖类型（IMPORT, USAGE, CALL, CREATION, ASSOCIATION等）", `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "依赖关系";
CREATE EDGE `documented_by` () ttl_duration = 0, ttl_col = "", comment = "注释关系";
CREATE EDGE `implemented_by` () ttl_duration = 0, ttl_col = "", comment = "方法实现关系";
CREATE EDGE `injection_calls` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "依赖注入调用关系";
CREATE EDGE `instance_of` ( `line_number` int64 NULL COMMENT "代码行号", `dependency_type` string NULL COMMENT "依赖类型（IMPORT, USAGE, CALL, CREATION, ASSOCIATION等）") ttl_duration = 0, ttl_col = "", comment = "实例化关系";
CREATE EDGE `interface_calls` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "接口方法调用关系";
CREATE EDGE `maps_to` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "实体对象整体映射关系（如BeanCopy）";
CREATE EDGE `out_calls` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "外部方法调用关系";
CREATE EDGE `overridden_by` () ttl_duration = 0, ttl_col = "", comment = "方法重写关系";
CREATE EDGE `passes_to` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "方法参数实体对象透传关系";
CREATE EDGE `reads_field` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "读取实体字段属性关系";
CREATE EDGE `subtype_calls` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "子类方法调用关系";
CREATE EDGE `super_calls` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "父类方法调用关系";
CREATE EDGE `writes_field` ( `line_number` int64 NULL COMMENT "代码行号") ttl_duration = 0, ttl_col = "", comment = "写入实体字段属性关系";
:sleep 20;

# Create Index:
CREATE TAG INDEX `annotations_repo_branch` ON `annotations` ( `repo_id`(256), `branch_name`(256));
CREATE TAG INDEX `class_repo_branch` ON `class` ( `repo_id`(256), `branch_name`(256));
CREATE TAG INDEX `comment_repo_branch` ON `comment` ( `repo_id`(256), `branch_name`(256));
CREATE TAG INDEX `field_repo_branch` ON `field` ( `repo_id`(256), `branch_name`(256));
CREATE TAG INDEX `file_repo_branch` ON `file` ( `repo_id`(256), `branch_name`(256));
CREATE TAG INDEX `function_repo_branch` ON `function` ( `repo_id`(256), `branch_name`(256));
CREATE TAG INDEX `idx_class_full_name` ON `class` ( `full_name`(256));
CREATE TAG INDEX `idx_function_entry_point` ON `function` ( `is_entry_point`);
CREATE TAG INDEX `idx_function_name` ON `function` ( `name`(256));