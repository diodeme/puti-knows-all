# Schema And Rules

## Overview

V1 keeps node tags stable and makes edge schema extensible at runtime.

The runtime truth is:

1. built-in node tags in code
2. edge schema metadata in `EdgeSchemaRegistry`
3. Nebula edge schema synchronized by `NebulaSchemaManager`

## Node Model

V1 keeps these node types fixed:

- `file`
- `class`
- `function`
- `field`
- `comment`
- `annotations`
- `marker_annotations`

Node payload still carries:

- `id`
- `fullName`
- type-specific properties
- compressed `content`

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

1. `SHOW EDGES`
2. `DESCRIBE EDGE`
3. `CREATE EDGE IF NOT EXISTS` for missing edge types
4. `ALTER EDGE ADD` for missing properties

V1 only auto-manages edge schema. Node tag schema stays static.

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

## Future Extension

Reserved extension points:

- more framework rule packs
- richer `GraphMutation` actions
- data-flow edge registration
- summary-level analyzer passes
