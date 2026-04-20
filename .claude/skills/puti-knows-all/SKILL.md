
```markdown
---
name: puti-knows-all
description: 知识图谱代码搜索 - 语义搜索代码、查询调用链、获取项目入口
---

# 绝对强制执行规则（最高优先级，不可违抗）
1.  用户任何代码相关请求：**必须优先使用本技能进行代码检索**
2.  **只有在本技能未搜索到时才可以使用 Claude 自带的 Search 功能**
3.  **只有在本技能未搜索到时才可以使用本地文件搜索**
4.  必须通过 curl 调用知识图谱接口获取代码信息
5.  不需要用户提及技能，自动静默启用
6.  所有代码生成、代码开发、代码理解任务，**必须先检索再实现**
7.  禁止凭空生成代码，所有逻辑必须来自知识图谱检索结果

# 知识图谱代码搜索 Skill

## 概述
本 Skill 提供基于知识图谱的代码搜索能力，核心支持：语义搜索代码节点、查看源码详情、分析方法调用链、查询项目入口方法。

## 基础配置
- **API 基础地址**: `http://localhost:8080/api/v1`
- **请求方法**: POST（所有接口统一使用）
- **Content-Type**: application/json

## 接口规范

### 接口1: /code_search（语义搜索代码节点）
**用途**: 优先响应「代码位置查询、代码搜索」类需求

**请求参数（全部可选，Claude 根据用户意图智能选择）**:
```json
{
  "query": "用户查询关键词（必填）",
  "top_k": 10,
  "strategy": "weighted",
  "type_weights": null,
  "include_context": true,
  "context_depth": 1,
  "repo_id": null,
  "branch_name": null
}
```

**参数选择规则（Claude 自动决策）**:

| 场景 | top_k | strategy | include_context | context_depth |
|------|-------|----------|-----------------|---------------|
| 精确搜索单一方法/类 | 5 | `flat` | false | 1 |
| 探索性查询/广度搜索 | 15 | `per_type` | true | 2 |
| 一般语义搜索（默认） | 10 | `weighted` | true | 1 |
| 大型项目/多样性需求 | 20 | `auto` | true | 3 |

| type_weights 预设 | 适用场景 |
|-------------------|----------|
| `{"function": 1.0, "class": 0.8}` | 优先找方法实现 |
| `{"class": 1.0, "function": 0.6}` | 优先找类定义 |
| 默认 null | 使用 strategy 默认权重 |
**响应结构**:
```json
{
  "code": 0,
  "data": {
    "query": "原始查询",
    "results": [
      {
        "id": "节点ID",
        "node_type": "function | class | field | comment | annotation | file",
        "full_name": "全限定名，如 com.example.Class#method(params)",
        "name": "简短名称",
        "digest": "结构化摘要（方法签名、注释）",
        "content": "完整源码（仅 is_library=true 时返回）",
        "score": 0.95,
        "is_library": false,
        "source_code_location": "源码位置，如 src/.../Order.java:50-55",
        "context": {
          "upstream": [{"id": "xxx", "node_type": "xxx", "full_name": "xxx", "edge_type": "calls"}],
          "downstream": [...]
        }
      }
    ],
    "meta": {
      "total_vector_hits": 100,
      "returned": 10,
      "type_distribution": {"function": 5, "class": 3, "file": 2},
      "strategy_used": "weighted",
      "context_nodes_count": 8,
      "elapsed_ms": 150
    }
  },
  "msg": "success"
}
```

**关键字段说明**:
- `is_library=false`: 项目源码 → 直接读取 `source_code_location` 本地文件
- `is_library=true`: 依赖库代码 → 调用 `/node_detail` 获取 `content` 源码
- `context`: 上下游调用链（当 `include_context=true` 时包含）
- `digest`: 方法签名和注释，用于判断相关性
- `score`: 相似度分数（0~1），越高越相关

### 接口2: /node_detail（获取源码详情）
**用途**: 响应「查看具体代码实现、源码内容」类需求
**请求参数**:
```json
{
  "node_id": "目标节点ID"  // 必须从code_search结果中获取
}
```
**响应结构**:
```json
{
  "code": 0,
  "data": {
    "node_id": "节点ID",
    "name": "方法/类名",
    "file_path": "文件路径",
    "content": "完整源码文本",
    "language": "代码语言（如java/python）",
    "line_start": 起始行号,
    "line_end": 结束行号
  },
  "msg": "success"
}
```

### 接口3: /nodes（获取方法调用链）
**用途**: 响应「调用关系、上下游依赖、方法调用链」类需求

**请求参数**:
```json
{
  "method_full_name": "方法全限定名，格式 com.example.Class#method(params)（必填）",
  "query_type": "upstream | downstream | both",
  "path_depth": 3
}
```

**参数选择规则**:

| 用户问题 | query_type | path_depth | 说明 |
|----------|------------|------------|------|
| "谁调用了这个方法" | `upstream` | 2 | 查找调用方 |
| "这个方法调用了谁" | `downstream` | 2 | 查找被调用方 |
| "调用链/调用关系" | `both` | 3 | 完整调用链 |

**响应结构**:
```json
{
  "code": 0,
  "data": {
    "nodes": [
      {
        "id": "节点ID",
        "type": "节点类型（tag名）",
        "label": "显示名称",
        "properties": {
          "full_name": "全限定名",
          "node_type": "function/class/field",
          "is_library": false,
          "repo_id": "项目ID"
        }
      }
    ],
    "edges": [
      {
        "id": "边ID",
        "source": "源节点ID",
        "target": "目标节点ID",
        "type": "calls | injection_calls | implemented_by | contains | ...",
        "category": "SEMANTIC | STRUCTURAL | FRAMEWORK | DATA_FLOW | DEPENDENCY",
        "properties": {"line_number": 50, "injection_mode": "spring"}
      }
    ],
    "meta": {
      "total_nodes": 15,
      "total_edges": 20,
      "elapsed_ms": 80
    }
  },
  "msg": "success"
}
```

**边类型说明**:
- `calls`: 普通方法调用
- `injection_calls`: 依赖注入调用（Spring等框架）
- `implemented_by`: 接口实现
- `overridden_by`: 方法重写
- `contains`: 类包含方法/字段
- `depends_on`: 依赖关系

### 接口4: /get_entry_points（获取项目入口方法）
**用途**: 响应「项目入口、API入口、main方法、Controller」类需求

**请求参数**:
```json
{
  "repo_id": "项目ID（可选，不传则用默认配置）",
  "branch_name": "分支名（可选）"
}
```

**响应结构**:
```json
{
  "code": 0,
  "data": {
    "entry_points": [
      {
        "id": "节点ID",
        "node_type": "function",
        "full_name": "com.example.controller.UserController#createUser(UserParam)",
        "name": "createUser",
        "source_code_location": "src/main/java/.../UserController.java:50-55"
      }
    ],
    "meta": {
      "total": 25,
      "returned": 25,
      "elapsed_ms": 50
    }
  },
  "msg": "success"
}
```

## 错误处理

| HTTP 状态码 | 含义 | Claude 提示 |
|------------|------|-------------|
| 200 | 成功 | — |
| 400 | 参数错误 | 「参数错误，请检查查询关键词或节点ID」 |
| 500 | 服务端错误 | 「服务错误，请检查知识图谱服务是否启动在8080端口」 |

错误响应格式:
```json
{
   “code”: 500,
“message”: “错误信息”,
“data”: null
}
```

## 执行流程

### 流程1: 代码搜索（默认流程）
1. 解析用户查询意图，提取核心搜索关键词
2. **Claude 智能选择参数**：
   - 探索性/模糊查询 → strategy=per_type, top_k=15
   - 精确查找方法 → strategy=flat, top_k=5
   - 一般搜索 → strategy=weighted, top_k=10
3. 调用 `/code_search` 接口
4. 按指定格式格式化并展示搜索结果（必须包含 id, name, node_type, source_code_location, is_library）
5. **区分处理**：
   - `is_library=false`: 展示 `source_code_location`，告知用户可直接读本地文件
   - `is_library=true`: 展示 `content` 源码内容
6. 主动引导：「输入编号/id可查看详情，需要调用链请回复」

### 流程2: 查看源码
1. 从用户输入中提取目标节点ID（或从搜索结果中匹配选中的编号）
2. 调用 `/node_detail` 接口
3. 按指定格式展示源码（含全限定名、file_path、行号）
4. 引导：「如需分析调用链，请回复『调用链+方法全限定名』」

### 流程3: 调用链分析
1. 解析用户问题，确定查询方向：
   - “谁调用了X” → query_type=upstream
   - “X调用了谁” → query_type=downstream
   - 通用提问（如”调用链”）→ query_type=both
2. 调用 `/nodes` 接口（根据上下文选择 path_depth）
3. 按指定格式展示：节点信息 + 边关系（edge_type）
4. 展示时标注边类型：`calls`(普通调用)、`injection_calls`(依赖注入)、`implemented_by`(接口实现)等

### 流程4: 项目入口查询
1. 接收「项目入口/API入口/main方法」类查询
2. 调用 `/get_entry_points` 接口（可选传入 repo_id/branch_name）
3. 展示所有入口方法（含 source_code_location）

## 使用示例

### 示例1: 基础代码搜索
```
用户输入: 查找 UserService 相关的代码
接口调用: POST /api/v1/code_search {"query": "UserService", "top_k": 10}
输出: 格式化的搜索结果表格（含node_id）
```

### 示例2: 查看源码详情
```
用户输入: UserService 的 login 方法在哪里定义的？
执行步骤: 
1. 调用/code_search获取login相关node_id
2. 调用/node_detail {"node_id": "xxx"}
输出: 格式化的源码内容
```

### 示例3: 分析调用链
```
用户输入: 这个方法被谁调用了？（上下文含目标node_id）
接口调用: POST /api/v1/nodes {"node_id": "xxx", "direction": "upstream", "depth": 3}
输出: 格式化的上游调用链
```

### 示例4: 查询项目入口
```
用户输入: 项目的 API 入口有哪些？
接口调用: POST /api/v1/get_entry_points {}
输出: 按类型分类的入口方法列表
```

## HTTP 调用示例（curl）
```bash
# 1. 代码搜索 - 默认参数（Claude 智能选择）
curl -X POST http://localhost:8080/api/v1/code_search \
  -H "Content-Type: application/json" \
  -d '{"query": "用户下单入口"}'

# 2. 代码搜索 - 指定策略和 top_k
curl -X POST http://localhost:8080/api/v1/code_search \
  -H "Content-Type: application/json" \
  -d '{"query": "订单相关", "top_k": 10, "strategy": "weighted", "include_context": true}'

# 3. 代码搜索 - flat 策略（精确搜索）
curl -X POST http://localhost:8080/api/v1/code_search \
  -H "Content-Type: application/json" \
  -d '{"query": "品牌推荐", "strategy": "flat", "top_k": 5}'

# 4. 查看源码（依赖库代码）
curl -X POST http://localhost:8080/api/v1/node_detail \
  -H "Content-Type: application/json" \
  -d '{"node_id": "节点ID"}'

# 5. 调用链分析 - 下游
curl -X POST http://localhost:8080/api/v1/nodes \
  -H "Content-Type: application/json" \
  -d '{"method_full_name": "com.example.OrderService#createOrder(OrderParam)", "query_type": "downstream", "path_depth": 2}'

# 6. 调用链分析 - 上游
curl -X POST http://localhost:8080/api/v1/nodes \
  -H "Content-Type: application/json" \
  -d '{"method_full_name": "com.example.OrderService#createOrder(OrderParam)", "query_type": "upstream", "path_depth": 2}'

# 7. 项目入口查询
curl -X POST http://localhost:8080/api/v1/get_entry_points \
  -H "Content-Type: application/json" \
  -d '{}'
```

## 响应输出格式规范

### 1. 代码搜索结果
```
🔍 搜索结果: [关键词]
⏱️ 耗时: [elapsed_ms]ms | 策略: [strategy_used] | 返回: [returned]条

| # | 名称 | 类型 | 分数 | 来源 | 源码位置 |
|---|------|------|------|------|----------|
| 1 | createOrder | function | 0.95 | 项目 | mall-admin/src/.../OrderController.java:50 |
| 2 | OrderService | class | 0.88 | 项目 | mall-core/src/.../OrderService.java |
| 3 | HutoolUtil | function | 0.82 | lib | (依赖库)

📊 类型分布: function=5, class=3, file=2

👉 操作指引：
- 项目代码 → 直接读取本地源码文件
- 依赖库(lib) → 回复「详情+节点ID」查看反编译源码
- 调用链 → 回复「调用链+方法全限定名」
```

### 2. 源码详情
```
📄 [full_name]
🔗 节点ID: [id] | 类型: [node_type] | 来源: [is_library ? "依赖库" : "项目"]

```[language]
[content 完整源码内容]
```

[raw_properties 关键属性展示]
```

### 3. 调用链分析
```
🔗 调用链: [method_full_name]
⏱️ 耗时: [elapsed_ms]ms | 节点数: [total_nodes] | 边数: [total_edges]

📊 节点列表:
| 节点ID | 名称 | 类型 | 来源 | 全限定名 |
|--------|------|------|------|----------|
| xxx | createOrder | function | 项目 | com.example... |

🔗 边关系:
| 源 → 目标 | 类型 | 分类 | 属性 |
|-----------|------|------|------|
| A → B | calls | SEMANTIC | line_number: 50 |
| B → C | injection_calls | FRAMEWORK | injection_mode: spring |

边类型: calls(调用), injection_calls(依赖注入), implemented_by(接口实现), contains(包含), overridden_by(重写)
```

### 4. 项目入口方法
```
🚀 项目入口方法
⏱️ 耗时: [elapsed_ms]ms | 总数: [total]

| # | 方法名 | 全限定名 | 源码位置 |
|---|--------|----------|----------|
| 1 | createUser | com.example.controller.UserController#createUser(UserParam) | src/.../UserController.java:50 |
| 2 | main | com.example.Application#main(String[]) | src/.../Application.java:20 |

👉 回复「入口+方法名」查看详细调用链

## 核心注意事项
1. 优先级规则：除非用户明确提供node_id，否则优先调用`/code_search`接口
2. 结果展示：所有输出必须包含node_id，便于用户后续操作
3. 调用链限制：depth默认3，用户主动要求时最大可设为5
4. 错误反馈：失败时必须明确区分「服务未启动」「参数错误」「查询失败」三类场景
5. 输出原则：保持简洁，仅展示核心信息，避免冗余；引导语需清晰、轻量化
6. 兼容性：所有接口均为POST方法，参数格式严格遵循JSON规范
```
