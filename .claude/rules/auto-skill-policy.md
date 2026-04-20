# 强制技能使用策略
agent_version: "1.0"
name: "code-navigation-policy"

# 全局规则：所有代码任务必须用知识图谱
rules:
  - trigger:
      - 代码开发
      - 代码生成
      - 代码搜索
      - 代码查询
      - 代码分析
      - 需求开发
      - 查找接口
      - 查找方法
      - 项目结构分析
    action:
      # 强制启用你的技能
      use_skill: "puti-knows-all"

  # 通用兜底规则：任何代码相关都走知识图谱
  - trigger:
      - code
      - search
      - find
      - implement
      - develop
      - query
      - api
      - route
    action:
      use_skill: "puti-knows-all"
      block_builtin_tools: [Search]

# 禁止行为
forbidden:
  - 不经过知识图谱直接生成代码
  - 使用本地文件搜索代替知识图谱
  - 凭空编写项目代码

# 优先级（最高）
priority: highest