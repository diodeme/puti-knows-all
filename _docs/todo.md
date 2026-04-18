feature
完成 1.前端展示相同全限定名的方法需要区分projectId和branchName
完成 2.混合检索写入
完成 2.2 混合检索查询
3.流程说明书和全局说明书
完成 4.注释->类->方法摘要 注释->方法实体
完成 5.新增入口节点配置规则
完成 6.新增说明书保存表
7.说明书向量匹配
8.roo code 流程编排
完成 9.语义搜索接口 (POST /api/v1/code_search) — 支持 weighted/auto/flat/per_type 四种策略
完成 10.入口方法查询接口 (POST /api/v1/get_entry_points)
完成 11.调用链双向查询 (POST /api/v1/nodes query_type=both)
完成 12.file_path 属性写入所有节点类型（构建阶段直接设置）
完成 13.Tag Schema 自动演进 (ALTER TAG ADD)


bug fix
完成 1.documentation_method带有引号
完成 2.全文检索只有特定场景才能匹配

impr
1.openai http请求改成响应式
2.图谱解析多线程
3.entrypoint：job、controller、责任链区分
4.需要看一下全文检索，驼峰和下划线式命名要不要拆分？
5.文档生成任务改成异步任务模式（引入xxljob）
6.文档生成支持upsert
7.多轮对话，是否需要init和final？
8.mall的entrypoint检查，是否需要增加job等
9.com.puti.code.documentation.repository.sql.impl.DocumentationMethodRepositoryImpl.saveAll 批量插入语句

说明书
1.说明书的摘要需要由模型生成，对摘要进行混合索引
2.入口说明书生成完后，由模型进行聚类，然后针对聚类生成交互说明书
3.需要解析出切面等逻辑，用于生成技术说明书


重构
存储层重构