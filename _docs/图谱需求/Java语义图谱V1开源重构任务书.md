# Java语义图谱 V1 开源重构任务书

## 1. 文档目标

本任务书用于定义 `maling-java-graph` 第一版开源前的最小重构范围、模块边界、交付标准与实施顺序。

本次重构目标不是一步到位做成通用代码图平台，而是在现有 Java + Spoon + Nebula + Server/Frontend 基础上，收敛出一个可以稳定开源、可以持续迭代、可以承接后续数据流分析的 V1 基线版本。

与“规则如何承载框架差异、如何支持梯度扩展”相关的抽象设计，统一见 [`语义扩展体系设计.md`](./语义扩展体系设计.md)。

---

## 2. V1 产品定位

### 2.1 定位

V1 定位为：

`Java 语义图谱工具（面向 AI 上下文构建与影响分析）`

处于以下两个生态位之间：

- Tree-sitter 类工具：语法级、轻量、结构化弱语义
- CPG 类工具：工业级、程序分析完备、控制流与数据流复杂

V1 明确不走 CPG 的重程序分析路线，也不止停留在语法树抽取层，而是聚焦：

- 文件、类、方法、字段、注解、注释等结构图谱
- 调用、包含、依赖、实现、重写、Spring Bean 关联等语义关系
- 面向 AI 使用场景的高价值业务语义增强

### 2.2 V1 不做的事情

以下内容不属于 V1 范围：

- 多语言支持
- 控制流图（CFG）
- 完整数据流图（DFG/PDG）
- 多数据库适配
- 插件热加载
- 增量解析
- 图谱版本管理
- 大规模前端重设计
- Quarkus 等其他框架的真实落地支持

### 2.3 V1 需要为后续预留的能力

虽然 V1 不直接交付数据流能力，但必须为后续 `spoon-dataflow` 接入预留扩展点：

- 可扩展 edge schema
- 可插拔 pass/pipeline
- 可产出图关系的规则系统
- 可承载 future data-flow edge 的查询协议
- 面向 rule pack 的语义扩展入口

---

## 3. V1 核心目标

V1 必须完成以下五条主线：

1. 固定节点 + 可扩展边
2. Nebula edge schema 自动管理
3. Spring 语义规则化增强
4. 分阶段、可扩展的分析 pipeline
5. 通用图查询与前端可视化协议

这五条主线同时覆盖：

- `code-graph-base`
- `code-graph-analyzer`
- `code-graph-rule`
- `code-graph-repository`
- `code-graph-app`
- `code-graph-server`
- `code-graph-frontend`

---

## 4. V1 交付定义

V1 开源基线完成后，应满足以下定义：

1. 可以解析 Java 项目并生成结构语义图谱
2. 节点类型保持稳定，边关系支持注册式扩展
3. Nebula 可以在启动或写入前自动发现并补齐缺失 edge schema
4. Spring 入口点与依赖注入语义通过规则增强产出，不再深度硬编码在单一处理器中
5. 分析过程具有清晰的阶段化边界，支持后续新增 pass
6. Server 返回统一图协议，前端可以展示未知 edge 类型
7. 文档足够让外部开发者跑通、理解并二次扩展

---

## 5. 当前主要问题

### 5.1 图模型层

- `NodeType`、`EdgeType` 仍是编译期枚举
- `Edge` 属性固定，无法自然承载后续数据流或框架语义属性
- edge schema 真相散落在枚举、DDL、写库逻辑三处

### 5.2 存储层

- Nebula DDL 文件是静态资产，运行时不会自动校验和补齐
- 写边逻辑假设只有 `line_number`、`dependency_type` 等少数字段
- 未来新增 edge type 或 edge 属性需要同时改模型、DDL、写库代码

### 5.3 分析层

- `DependencyInjectionProcessor` 过大，Spring 语义与底层事实提取耦合严重
- `ExecutableProcessor` 职责过多，已经承担多类分析逻辑
- 主流程通过 `AbstractHandler` 手工串处理器，扩展点不清晰

### 5.4 规则层

- 当前规则系统主要只覆盖入口点识别
- 规则输出是 `Boolean`，无法直接产出关系或属性增强
- 依赖注入、RPC、框架语义等仍未规则化

### 5.5 查询与可视化层

- Server DTO 仍偏向当前固定图模型
- 前端对于 edge type 和属性的扩展容忍度有限
- 若后端引入新 edge 类型，前后端存在失配风险

---

## 6. V1 总体改造原则

### 6.1 原则一：节点稳定，边先开放

V1 不追求节点类型动态化。

V1 选择：

- 节点类型继续固定
- 边类型与边属性改为注册式 schema

理由：

- 节点类型在 V1 阶段相对稳定
- 未来语义演进主要集中在 edge
- 这样能以最小复杂度换取最大扩展收益

### 6.2 原则二：事实提取与语义增强分层

底层解析只负责提取“事实”：

- 声明
- 引用
- 注解
- 注解参数
- 调用
- 继承/实现
- Bean 定义点
- 注入点

框架语义增强由规则或专门增强 pass 完成。

进一步约束见 [`语义扩展体系设计.md`](./语义扩展体系设计.md)：

- 小需求优先只改 rule pack
- 中等需求再引入 custom function
- 只有新分析能力才新增 semantic pass

### 6.3 原则三：运行时 schema 以 registry 为准

DDL 文件可以继续存在，但只作为：

- 初始化样例
- 文档说明
- 手工部署参考

运行时真实 schema 应由代码中的 registry + Nebula schema manager 管理。

### 6.4 原则四：前后端协议优先稳定

V1 不追求前端华丽重构，但必须先稳定图协议，让：

- 新 edge 能显示
- 新属性能查看
- 新类型能过滤

否则后续每次增强图模型都会连带打崩前端。

---

## 7. V1 目标架构

### 7.1 分层结构

建议收敛为以下分层：

1. `Parser Layer`
   - Spoon 解析 Java AST

2. `Fact Extraction Layer`
   - 提取结构事实与基础关系

3. `Semantic Enhancement Layer`
   - Spring 规则增强
   - 入口点增强
   - Bean 绑定增强
   - 后续预留 data flow 增强

4. `Persistence Layer`
   - Nebula schema 管理
   - 图写入
   - 向量写入

5. `Query/Presentation Layer`
   - 图查询服务
   - 图协议转换
   - 前端展示与过滤

### 7.2 Pipeline 阶段建议

V1 统一为以下阶段：

1. `SourceParseStage`
2. `FactExtractStage`
3. `SemanticEnhanceStage`
4. `PersistStage`
5. `VectorStage`

其中：

- `VectorStage` 可保留为可选阶段
- 后续接入 data flow 时可新增 `DataFlowEnhanceStage`

对于 DI 等框架语义，推荐采用“两轮解析 + 一步落图”的统一模型：

1. `Collect Facts`
2. `Resolve Semantics`
3. `Emit Graph Mutations`

具体分层见 [`语义扩展体系设计.md`](./语义扩展体系设计.md)。

---

## 8. 模块级任务拆解

## 8.1 `code-graph-base`

### 必做任务

1. 保持 `NodeType` 固定，不做动态化
2. 将 `Edge` 从固定字段模型改为：
   - `type`
   - `properties`
   - `category`
3. 新增 edge schema 元数据模型：
   - `EdgeSchema`
   - `EdgePropertySchema`
   - `EdgeCategory`
4. 新增 edge schema registry：
   - 内建默认边注册
   - 支持后续规则或增强 pass 注册新边
5. 保持节点模型兼容现有 Server/Frontend

### 目标产物

- 边模型从枚举依赖转为注册依赖
- 为后续数据流 edge 预留类型空间

### 建议边分类

- `STRUCTURAL`
- `SEMANTIC`
- `FRAMEWORK`
- `DATA_FLOW`

---

## 8.2 `code-graph-repository`

存储层详细设计见 [`图存储层设计.md`](./图存储层设计.md)。

### 必做任务

1. 新增 `NebulaSchemaManager`
2. 支持以下 schema 管理能力：
   - `SHOW EDGES`
   - `DESCRIBE EDGE`
   - `SHOW CREATE EDGE`
   - `CREATE EDGE IF NOT EXISTS`
   - `ALTER EDGE ADD`
3. 启动时对 registry 中 edge schema 做校验与补齐
4. 给 schema 做本地缓存，避免每次写边都查询图库
5. 重构 `NebulaGraphClient` 写边逻辑：
   - 不再假设只有固定属性名
   - 根据 edge schema 和 edge properties 动态拼装
6. 节点 tag 本次仍维持静态 schema，不纳入动态化范围

### 设计要求

- schema 变更失败要有清晰日志
- 对 Nebula schema 异步生效做等待或确认机制
- 对不存在 schema 的写入要有显式保护

### 目标产物

- edge schema 自动管理
- DDL 文件从唯一真相降级为参考资产

---

## 8.3 `code-graph-rule`

### 必做任务

1. 规则系统从“布尔判断”升级为“可产图增强”
2. V1 至少支持以下规则能力：
   - `match`
   - `when`
   - `emitEdge`
   - `emitProperty`
   - `priority`
3. 保持 `entryPointRules`，并新增真正可执行的：
   - `dependencyInjectionRules`
4. 把规则输出定义成统一结果对象，例如：
   - `RuleMatchResult`
   - `GraphMutation`
5. 扩充 `RuleContext`，至少提供：
   - 注解名
   - 注解参数
   - 类注解
   - 实现接口
   - 继承链
   - 方法签名
   - 参数类型
   - 返回类型
   - 字段注入点信息

### 设计约束

V1 的规则系统不追求“全配置替代代码”，而是优先承担：

- 语义映射
- 绑定优先级
- mutation 决策

不承担：

- AST 遍历
- 类型求解
- 符号绑定
- 数据流求解

普通开源用户的默认扩展路径应为：

1. `rule pack`
2. `custom function`
3. `semantic pass`

而不是一上来就要求理解完整内核抽象。

### V1 规则覆盖范围

- 入口点识别
- `@Autowired/@Qualifier/@Resource/@Bean` 关联
- Spring 常见组件识别

### V1 对扩展体系的目标

对于共享同一 DI 元模型的框架差异，例如 Spring Boot 与 Quarkus/CDI，目标应当是：

`优先只通过 rule pack 抹平差异，不修改核心 DI 流程。`

详细设计见 [`语义扩展体系设计.md`](./语义扩展体系设计.md)。

### 不属于 V1 的规则

- RPC 框架规则
- 消息队列规则
- 事务传播规则
- 多框架并行支持

---

## 8.4 `code-graph-analyzer`

### 必做任务

1. 拆分当前“结构处理”和“框架处理”边界
2. 将 `DependencyInjectionProcessor` 拆为两层：
   - 事实提取层
   - 规则增强层
3. 收敛 `ExecutableProcessor` 职责，避免继续堆积框架逻辑
4. 对外输出稳定事实对象，而不是直接写死某种框架语义边
5. 引入阶段化 pass 执行机制

### 设计约束

`code-graph-analyzer` 需要逐步从“框架专有 processor”收敛到“通用事实提取 + 语义解析”的结构。

其中：

- 第一轮以 typed facts 为主输出
- 第二轮以 resolution 结果为主输出
- 最后再由 mutation emitter 落图

这意味着 analyzer 不应继续把 Spring 专有 if/else 作为主要扩展方式。

### 推荐拆分方向

- `StructureFactExtractor`
- `AnnotationFactExtractor`
- `InvocationFactExtractor`
- `BeanFactExtractor`
- `EntryPointEnhancer`
- `DependencyInjectionEnhancer`

### V1 保留的基础语义

- `contains`
- `calls`
- `out_calls`
- `depends_on`
- `implemented_by`
- `overridden_by`
- `documented_by`

### V1 改由增强规则产出的语义

- `injection_calls`
- `instance_of` 中的 Bean 绑定语义部分
- 入口点标记
- 策略模式 Bean Map 绑定

### 对数据流的预留要求

V1 虽不接入 `spoon-dataflow`，但分析链必须允许未来新增：

- `DataFlowFactExtractor`
- `DataFlowSummaryEnhancer`
- `DataFlowEdgeEmitter`

---

## 8.5 `code-graph-app`

### 必做任务

1. 将当前 `AbstractHandler` 改造为 pipeline 驱动
2. 将处理阶段明确注册，避免主流程硬编码处理器顺序
3. 提供统一入口用于：
   - 结构图构建
   - 语义增强构建
   - 后续数据流增强接入
4. 保持现有命令行运行模式

### 目标产物

- 主流程职责变成 orchestration
- 新增 pass 不需要频繁修改主流程

---

## 8.6 `code-graph-server`

### 必做任务

1. 定义统一图响应协议
2. Server DTO 从“当前写死图模型”收敛为“通用图协议”
3. 图查询结果至少统一输出：
   - `nodes[]`
   - `edges[]`
   - `meta`
4. `GraphEdge` 需要支持：
   - `type`
   - `category`
   - `properties`
5. `GraphNode` 需要支持通用属性字典
6. 查询服务不要对 edge type 做过多硬编码特判

### 建议图协议

#### Node

- `id`
- `type`
- `label`
- `properties`

#### Edge

- `id`
- `type`
- `category`
- `source`
- `target`
- `properties`

#### Meta

- `nodeTypeStats`
- `edgeTypeStats`
- `legend`
- `queryInfo`

### 目标产物

- 后端图查询协议稳定
- 新 edge 类型不会天然打崩 API 结构

---

## 8.7 `code-graph-frontend`

### 必做任务

1. 前端按通用图协议消费数据
2. 引入图样式配置，而非把 edge type 逻辑散落在组件中
3. 未知 edge type 必须具备默认展示能力
4. 详情面板按属性字典渲染
5. 过滤器支持：
   - 节点类型
   - 边类型
   - 边分类
   - 深度

### V1 不做的事情

- 不大改整体视觉风格
- 不重写整套交互架构
- 不做复杂可视分析工作台

### 目标产物

- 后端新增 edge 类型后，前端最多改配置，不必改核心渲染逻辑

---

## 9. V1 推荐目录与概念调整

### 9.1 新增概念

建议引入以下概念对象：

- `EdgeSchema`
- `EdgeSchemaRegistry`
- `GraphMutation`
- `RuleMatchResult`
- `NebulaSchemaManager`
- `GraphResponseMeta`
- `GraphStyleConfig`
- `RulePack`
- `SemanticPass`
- `CustomFunctionRegistry`

### 9.2 命名调整建议

建议逐步避免以下命名误导：

- `ParseSupport` 中混入 Spring 语义判断
- `DependencyInjectionProcessor` 同时承担事实提取与语义推断
- `AbstractHandler` 同时承担初始化、编排、执行细节

建议名称更贴近职责：

- `JavaSemanticPipeline`
- `BeanFactExtractor`
- `SpringSemanticEnhancer`
- `GraphProtocolMapper`

---

## 10. V1 验收标准

### 10.1 Analyzer 验收

- 能对标准 Spring Boot Java 项目完成图谱构建
- 基础结构边产出稳定
- Spring entry point 可识别
- Bean 注入关系可通过规则增强产出

### 10.2 Repository 验收

- 新增 edge schema 时无需手工修改核心写库逻辑
- 缺失 edge schema 时可自动创建或补字段
- 写库失败日志能明确定位 schema 问题

### 10.3 Server 验收

- 返回统一图协议
- 新 edge 类型在 API 层可透出
- 查询接口对 edge 属性保持兼容

### 10.4 Frontend 验收

- 能展示未知 edge 类型
- 节点与边详情支持通用属性显示
- 图例和筛选可以基于后端返回类型工作

### 10.5 开源工程验收

- README 清晰说明定位、范围与非目标
- 提供最小运行说明
- 提供 schema 和规则说明文档
- 提供一个 demo 项目与示例图谱

---

## 11. V1 文档交付要求

V1 除代码外，至少补齐以下文档：

1. `README.md`
   - 产品定位
   - 快速开始
   - 架构概览
   - 支持范围
   - 非目标

2. `docs/schema-and-rules.md`
   - 节点模型
   - edge schema
   - 规则系统

3. `docs/open-source-roadmap.md`
   - V1
   - V1.x
   - V2 方向

4. `docs/server-protocol.md`
   - 图查询协议
   - 前后端约定

5. `_docs/图谱需求/语义扩展体系设计.md`
   - 规则与代码的职责边界
   - 梯度扩展模型
   - DI 两轮解析模型
   - DFG 复用边界
   - 二次开发者接入流程

---

## 12. 实施顺序

建议按照以下顺序推进：

### 第一阶段：边模型与存储基座

- 重构 `Edge`
- 引入 `EdgeSchemaRegistry`
- 引入 `NebulaSchemaManager`
- 改造 Nebula 写边逻辑

### 第二阶段：分析层拆层

- 拆 `DependencyInjectionProcessor`
- 收敛 `ExecutableProcessor`
- 将 Spring 语义改为增强 pass

### 第三阶段：规则系统升级

- 扩展 `RuleContext`
- 支持 `emitEdge`
- 落地 `dependencyInjectionRules`
- 收敛 rule pack / custom function / semantic pass 三层扩展路径

### 第四阶段：Server/Frontend 协议收敛

- 定义统一图协议
- 改造 Server DTO
- 改造前端通用渲染与默认样式

### 第五阶段：开源化整理

- README 重写
- 增补设计文档
- 准备 demo 与截图

---

## 13. 建议里程碑

### 里程碑 M1：图模型基座完成

完成标志：

- 可注册 edge schema
- Nebula 可自动补齐 edge schema
- 写边逻辑不再依赖固定属性

### 里程碑 M2：Spring 语义规则化完成

完成标志：

- 入口点与依赖注入通过规则增强产出
- 大处理器完成拆层

### 里程碑 M3：查询协议稳定化完成

完成标志：

- Server 返回通用图协议
- Frontend 支持未知 edge 展示

### 里程碑 M4：V1 开源发版完成

完成标志：

- 文档齐备
- demo 可运行
- 发布范围和非目标清晰

---

## 14. 需要显式延期到 V1.x 或 V2 的事项

以下事项建议明确延期，不在 V1 内消耗主资源：

- Quarkus 支持
- 多语言支持
- `spoon-dataflow` 正式接入
- 函数级数据流摘要
- 语句级节点建模
- 高级影响链分析
- 前端复杂交互分析台

---

## 15. 风险与控制

### 风险一：重构范围失控

控制方式：

- 节点模型不动态化
- 不接入数据流真实能力
- 不扩展到多语言

### 风险二：规则系统过度设计

控制方式：

- V1 只做两类规则：入口点、依赖注入
- 只做最小产图能力，不做通用 DSL 大而全
- 默认扩展路径优先 rule pack，不强迫用户一上来引入新 fact / pass

### 风险三：前后端适配成本被低估

控制方式：

- 先统一协议，再做 UI 层兼容
- 前端优先配置化，不做大重写

### 风险四：Nebula schema 变更时序问题

控制方式：

- 启动预热 schema
- 对 schema 变更增加确认逻辑
- 日志明确区分 schema 问题与业务写入问题

---

## 16. 本任务书对应的最终 V1 结论

V1 的最小但完整的开源重构工作，应定义为：

`固定节点 + 可扩展边 + Spring 规则增强 + 可插拔分析 pipeline + 通用图查询/可视化协议`

这一定义满足以下要求：

- 工作量不至于过小，能够真正定住大方向
- 不会一步走到过度平台化
- 能承接后续数据流分析
- 能兼顾现有 Server 与 Frontend 的稳定演进
- 具备作为第一版开源项目发布的完整性

---

## 17. 建议的后续执行方式

建议将本任务书继续拆为以下三个执行文档：

1. `V1-必做任务拆解.md`
2. `V1-技术设计草案.md`
3. `V1-里程碑排期.md`

本任务书作为总纲，不直接承载逐日开发计划。

---

## 18. 当前实施状态

截至当前版本，V1 任务完成情况如下。

### 已完成

- [x] 固定节点 + 可扩展边模型
- [x] `EdgeSchemaRegistry` 与内建 edge schema 注册
- [x] `NebulaSchemaManager` 与 edge schema 自动补齐
- [x] `NebulaGraphClient` 动态写边
- [x] 入口点规则保留
- [x] 依赖注入规则增强最小闭环
- [x] `DependencyInjectionProcessor` 增强层拆分
- [x] `AbstractHandler` 改为 pipeline 驱动
- [x] Server 通用图协议
- [x] Frontend 未知 edge 类型默认展示
- [x] Frontend 图样式配置化
- [x] README 与开源文档补齐
- [x] 核心单测补齐

### 已验证

- [x] Java 编译通过
- [x] edge schema / rule / pipeline / server mapper / controller 单测通过
- [x] 前端生产构建通过

### 当前仍保留的非阻塞项

- [ ] 现有 Nebula 集成测试全量回归
- [ ] 既有前端 ESLint warning 清理
- [ ] 更彻底的 `ExecutableProcessor` 职责收敛
- [ ] 将 DI 扩展进一步收敛到“rule pack 优先”的统一元模型

### 明确延期项

以下事项仍按本任务书原定义延期到 V1.x 或 V2：

- [ ] `spoon-dataflow` 正式接入
- [ ] 函数级数据流摘要
- [ ] 语句级节点建模
- [ ] Quarkus 支持
- [ ] 多语言支持
