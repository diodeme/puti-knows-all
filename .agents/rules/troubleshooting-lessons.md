# Maling Graph 项目排障要点

本文件只保留可复用结论，优先记录“现象 -> 判断 -> 做法”。

## 1. 测试与样例工程
- 测试解析特定项目时，显式覆写并清空 `AppConfig.globalLibraryPath`，同时设置 `targetPackage`；否则既可能把无关 JAR 带进 Spoon，也可能把业务类整批过滤掉。
- 只验证本地逻辑的测试不要用 `@SpringBootTest`；优先纯单测或更窄的 slice，避免日志、数据源、AI 客户端等外围依赖污染结果。
- 真实连 Nebula 或外部样例工程的测试不要混入默认 `test`；用 `Assumptions`、`@EnabledIfSystemProperty` 或环境变量做 opt-in。
- 外部样例工程若通过 `external/` 下目录链接管理，默认配置、测试路径、脚本都统一引用 `external/...`，不要混用仓库外绝对路径。
- 测试 stub 也是语义输入；注解 stub 的包名、文件路径、import 和断言必须与真实规则一致。

## 2. Nebula 查询、Schema 与存储
- 文本匹配依赖索引；`CONTAINS` 之类条件不要和无索引扫描混用。查询成功但结果为空时，先怀疑索引和条件组合。
- 读取 `ResultSet` 属性前先判类型；`NULL` 直接 `asString()` 会触发 `InvalidValueException`。
- `CREATE/ALTER EDGE` 成功不代表 schema 已立即可写；DDL 后轮询 `SHOW/DESCRIBE`，确认 edge 和属性可见再写入。
- 动态 edge 写入前过滤 `null` 属性，避免把空字段名带进 `INSERT EDGE` 导致 `Unknown column`。
- `Edge.builder()` 同时支持 `lineNumber/dependencyType` 这类便捷 setter 和 `properties(...)` 时，`properties(...)` 必须做 merge 而不是整表覆盖；否则调用顺序一变，前面挂上的动态属性会被静默抹掉。
- 图查询的 edge type 列表和 category 推导跟随 `EdgeSchemaRegistry`，不要在 DAO 或 Service 手写白名单。
- Analyzer 与 Server 若分进程运行，不要指望运行时注册的 edge schema 自动同步到查询端内存；子图遍历优先 `OVER *`，edge 分类再由返回属性或 `EdgeSchemaRegistry` 兜底。
- `GET SUBGRAPH` 这类方向子图查询不要直接拼 `OUT *` / `IN *` 然后把失败结果当空图吞掉；优先先查 `SHOW EDGES` 拿真实 edge type 列表再拼查询，并在 Service 层显式记录 `resultSet.getErrorMessage()`。
- 图谱主链只以 AST 解析和 Nebula 落库为成功标准；Milvus、Embedding 等外围能力必须可关闭。
- 做图存储可插拔时，把“写入接口、查询接口、schema 管理、方言翻译”拆开；如果 `ResultSet` 或 nGQL 继续泄漏到 analyzer / service，上层仍然会被 Nebula 锁死，所谓 config 切换只是表面解耦。
- 多后端图存储里的“通用 DDL”应理解为逻辑 schema 通用，不是同一份物理 DDL 直接跑 Nebula / Neo4j / 文件；共享真相放在 `EdgeSchemaRegistry`，各后端自己做 translator 或 no-op。
- JSONL 落盘不要直接复用 pretty-print JSON 工具；单条记录被格式化成多行后，会破坏 `jsonl` 协议并让回放、diff、批处理都变得不可靠。
- 不给 `local_file` 单独配 `storage.path` 时，落盘目录必须从稳定配置推导，例如 `project.root_path`；不要直接绑 `user.dir`，否则 analyzer、server、documentation 分进程运行时会各读各的目录。
- 动态 edge 类型不要在 analyzer、server、documentation 各自手写 `EdgeType -> EdgeSchemaRegistry -> getOrCreate` 解析链；统一抽一个 `EdgeDefinitionResolver`，否则 category/displayName/自动注册策略很快会漂移。
- 查询侧如果既要算 edge `category` 又要算展示名，不要分别各写一遍 registry fallback；两处都复用同一个 `EdgeDefinitionResolver`，否则“分类能识别、展示名识别不到”这类分叉问题很容易出现。
- `graph.storage.type` 这类后端选择配置不要把未知值静默回退到默认实现；配置拼错时如果仍偷偷落回 `nebula`，调用方会误以为抽象层切换成功，实际数据却还在写默认后端。
- 面向 server/documentation 的图查询适配层不要再直接 import Nebula `Node`/`Relationship`/`ValueWrapper`；Nebula 专用值格式化器放到 repository 或专项调试工具里，否则查询 DTO 虽然抽象出来了，上层模块仍会在编译期被 Nebula SDK 锁死。

## 3. DI、规则与语义解析
- Provider 候选登记、注入点提取、优先级排序、字段绑定、调用重定向要共享同一套解析结果；不要在多个 processor 里各维护一套 Spring 规则。
- “框架注解是否跳过”也复用同一份 `factMapping` / 解析支持类，不要在 `ClassProcessor`、`ExecutableProcessor` 回流硬编码白名单。
- 默认 rule pack 放资源文件，`RuleConfiguration` 统一从 classpath 读取；Java fallback 只保留空的通用默认对象。
- 扩展规则 DSL 时同步更新四处：规则定义与 evaluator、YAML 配置解析、默认 `rules.yml`、测试样例或辅助构造器。
- DI 流程按两轮执行：第一轮收 provider 和 injection facts，第二轮在 provider 收全后统一发字段绑定和调用重定向边，避免遍历顺序导致结果漂移。
- 如果对外承诺“rule pack 优先扩展”，`DependencyInjectionFact -> RuleContext` 不能只透出命中布尔值；至少把注入注解、注解属性、owner class annotations、接口/继承链、方法签名和参数类型一起带上，否则规则层无法真正承接框架差异。
- `RuleContext` 新增字段后，不只 builder/fact adapter 要补值，SpEL 变量注册层也要同步暴露同名变量；否则代码里“支持了 `methodSignature/superClassPath`”，规则表达式里却仍然取不到。
- 如果规则引擎承诺支持 `emitProperty` 这类 property-only mutation，规则定义对象的默认 `edgeType` 不能写死；否则 YAML 不配 `emitEdge` 也会被错误当成 edge mutation。

## 4. 构建、命令与环境
- Gradle Wrapper 若使用工作区外缓存目录，锁文件失败通常是权限问题，不是业务编译错误；优先检查 `GRADLE_USER_HOME` 和 wrapper dists 路径。
- Windows `cmd /c` 设置临时环境变量时统一写 `set "KEY=value" && ...`，避免尾随空格污染路径。
- Fat Jar 手工展开 `runtimeClasspath` 时，所有被打入包的模块都要在 `jar.dependsOn(...)` 显式声明对应 `jar` 任务依赖。
- 不要并行执行会编译同一子模块的多个 `gradlew` 命令，避免共享 `build/classes` 目录竞争。
- 受限环境执行前端构建时，把 `TEMP`、`TMP`、`HOME`、`USERPROFILE` 指到工作区内可写目录。
- 持有底层连接池/Session 的 repository Bean 统一实现 `AutoCloseable`（或显式 destroy-method），让 Spring 在 shutdown 时释放资源；否则 server/documentation 常驻进程里 `NebulaGraphClient` 这类连接会静默泄漏到进程退出。
- 排障不要只看表层断言；Spoon、`AbstractHandler`、Webpack/Node 基础库等底层日志，往往更早暴露真实故障点。

## 5. 协议、前端与跨端数据
- 跨端 DTO 字段名显式对齐；前后端命名风格不一致时，用 `@JsonProperty` 锁定协议，不要依赖默认 camelCase。
- 内容压缩协议必须统一复用 `ContentCompressor` 对应的 `Base64 + Inflater/zlib` 方案，不要在业务层混入另一套 `GZIPInputStream`。
- 会影响主视图请求的筛选条件属于页面级状态，不要只保存在局部搜索组件里。
- 后端子图接口保持通用图协议，不要为了当前页面只展示 `function` 就在查询服务层把其他节点或边删掉；页面展示范围在前端适配层控制。
- 前端节点标题不要假设 `properties.name` 必有值；统一通过展示名 accessor 按 `label -> name -> properties.label -> simple_name -> full_name/qualified_name -> id` 兜底，根节点识别也复用同一兼容逻辑。
- 动态 edge 的中文展示名和分类中文名不要只写死在前端映射；Server 要统一下发 `edgeTypeLabels`、`edgeCategoryLabels`，前端只保留 fallback。
