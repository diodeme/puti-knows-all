# Open Source Roadmap

## V1

Focus:

- Java only
- fixed nodes, extensible edges
- Spring semantic enhancement
- Nebula edge schema auto-sync
- Nebula tag schema auto-evolution
- generic graph protocol for server/frontend

Delivered themes:

- runtime edge schema registry
- pipeline-based analyzer orchestration
- dependency-injection rule evaluator
- generic graph API payload

## V1.1 — AI Coding Query Service (Delivered)

Semantic code search with vector similarity and graph context.

Delivered:

- `POST /api/v1/code_search` — vector semantic search with 4 strategies (weighted/auto/flat/per_type)
- `POST /api/v1/get_entry_points` — project entry point methods
- `POST /api/v1/nodes` — call chain query with `both` direction support
- `POST /api/v1/node_detail` — node detail with decompressed source code
- `file_path` property on all node types (function/class/comment/annotation/field)
- `MilvusSearchClient` — Milvus vector search with dense IP metric
- `CodeSearchService` — strategy application, node enrichment, context building
- Tag schema auto-evolution via `ALTER TAG ADD` in `NebulaSchemaManager`

Key files:

- `code-graph-server/.../controller/CodeSearchController.java`
- `code-graph-server/.../service/CodeSearchService.java`
- `code-graph-repository/.../milvus/MilvusSearchClient.java`
- `code-graph-analyzer/.../processor/BaseProcessor.java` (`resolveFilePath`)

## V1.x

Planned incremental work:

- Claude Code Skill integration (`.claude/skills/code-search/`)
- MCP service for direct agent tool calling
- automatic project dependency resolution and analysis (per-project `.library/`, zero-invasion Gradle `--init-script`)
- stronger server/controller protocol tests
- Nebula integration regression hardening
- better frontend graph legend and filters
- richer rule packs for Spring MVC and bean resolution
- cleaner analyzer responsibility split
- analyzer multi-threading
- more entry point rules (Job, Chain of Responsibility)

## V2 Direction

Explicitly outside V1 but aligned with the architecture:

- `spoon-dataflow` integration
- summary-level data-flow edges
- richer AI-oriented impact analysis
- statement-level graph experiments
- additional framework packs
- multi-language support beyond Java
