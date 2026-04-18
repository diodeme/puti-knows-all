# Schema And Rules

## Overview

V1 keeps node tags stable and makes edge schema extensible at runtime.

The runtime truth is:

1. built-in node tags in code
2. edge schema metadata in `EdgeSchemaRegistry`
3. Nebula edge schema synchronized by `NebulaSchemaManager`
4. node tag schema auto-evolved via `ALTER TAG ADD` in `NebulaSchemaManager`

## Node Model

V1 keeps these node types fixed:

- `file`
- `class`
- `function`
- `field`
- `comment`
- `annotations`
- `marker_annotations`

Node payload carries:

- `id`
- `fullName`
- type-specific properties
- compressed `content`
- `file_path` (relative source file path, populated for all types during analyzer phase)

### file_path Property

All node types store `file_path` as a direct property. This is set during the analyzer build phase using Spoon's `element.getPosition().getFile()` to extract the source file, then converted to a project-relative path via `AppConfig.getRelativePath()`.

The `resolveFilePath()` method in `BaseProcessor` provides this extraction for all processors, ensuring every function, class, comment, annotation, and field node carries its originating file path.

Schema evolution is automatic: `NebulaSchemaManager` compares existing tag properties (via `DESCRIBE TAG`) against expected definitions (from `NodeType` enum), and issues `ALTER TAG ADD` for any missing properties including newly added `file_path`.

## Edge Model

Each edge contains:

- `type`
- `category`
- `properties`

Categories:

- `STRUCTURAL`
- `SEMANTIC`
- `FRAMEWORK`
- `DATA_FLOW`

Built-in edge schemas are registered from `EdgeType`.

Examples:

- `contains`
- `calls`
- `depends_on`
- `implemented_by`
- `overridden_by`
- `injection_calls`
- `reads_field`
- `writes_field`
- `maps_to`
- `passes_to`

## Runtime Schema Sync

Nebula synchronization flow:

1. `SHOW TAGS` / `SHOW EDGES`
2. `DESCRIBE TAG` / `DESCRIBE EDGE`
3. `CREATE TAG IF NOT EXISTS` for missing tags
4. `ALTER TAG ADD` for missing tag properties
5. `CREATE EDGE IF NOT EXISTS` for missing edge types
6. `ALTER EDGE ADD` for missing edge properties
7. `CREATE TAG INDEX IF NOT EXISTS` + `REBUILD TAG INDEX` for indexed properties

Both tag and edge schemas are auto-managed.

## Rule System

V1 has two rule tracks:

1. `entryPointRules`
2. `dependencyInjectionRules`

Current rule outputs:

- boolean match for entry-point identification
- `GraphMutation` for graph enhancement

Key rule objects:

- `RuleContext`
- `RuleMatchResult`
- `GraphMutation`

## Dependency Injection Rules

`dependencyInjectionRules` are used to convert injection facts into framework edges.

Current default behavior:

- match `@Autowired` / `@Resource`
- optionally use qualifier presence
- emit framework binding edges such as `instance_of`

## Tag Index Definitions

The following tag indexes are maintained automatically:

| Tag | Property | Type |
|-----|----------|------|
| `function` | `full_name` | string(256) |
| `function` | `name` | string(128) |
| `function` | `repo_id` | string(64) |
| `function` | `branch_name` | string(64) |
| `function` | `is_entry_point` | bool |

## Future Extension

Reserved extension points:

- more framework rule packs
- richer `GraphMutation` actions
- data-flow edge registration
- summary-level analyzer passes
