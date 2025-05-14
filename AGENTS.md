# AGENTS 指南（Maling Graph）

本文件用于索引项目规则。开发与排障时请先查阅对应规则，并将新增经验登记到对应 rule 文件中。

## Rule 索引

1. `.agents/rules/meta_rule.md`
- 用途：元规则，你必须要遵守的规则
- 登记要求：你只有读取权限，没有修改权限

1. `.agents/rules/java-guide.md`
- 用途：Java 编码规范（Lombok 使用、日志写法、import 规范等）。
- 登记要求：Java 代码风格与实现约束新增/变更时，登记到本文件。

1. `.agents/rules/troubleshooting-lessons.md`
- 用途：项目开发与集成测试踩坑教训汇总（配置、查询、日志排查、DTO 映射等）。
- 登记要求：新增排障经验、根因与修复方式时，统一追加到本文件。

## 维护约定

- 不要把规则散落在临时文档或对话里，必须落盘到对应 rule 文件。
- 新增规则时，先在 `.agents/rules/` 新建或更新对应文件，再同步更新本 `AGENTS.md` 索引。
- 若同一事项涉及多个领域：
- 编码规范写入 `java-guide.md`
- 过程性要求写入 `meta_rule.md`
- 踩坑与案例写入 `troubleshooting-lessons.md`
