# Server Protocol

## Endpoints

### Graph Query — Call Chain

`POST /api/v1/nodes`

Returns graph nodes and edges for a method's call chain.

**Request body**:

```json
{
  "method_full_name": "com.example.UserService#createUser(String)",
  "query_type": "both",
  "path_depth": 1
}
```

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `method_full_name` | yes | — | Method fully qualified name |
| `query_type` | no | `"both"` | `upstream` (IN), `downstream` (OUT), `both` (merge both directions) |
| `path_depth` | no | 1 | Traversal depth, -1 for unlimited (capped at 10) |

**Response**:

```json
{
  "nodes": [
    {
      "id": "node-id",
      "type": "function",
      "label": "createUser",
      "properties": {
        "full_name": "com.demo.UserService#createUser()",
        "visibility": "public",
        "source_node": true
      }
    }
  ],
  "edges": [
    {
      "id": "src->dst:calls",
      "source": "src",
      "target": "dst",
      "type": "calls",
      "category": "SEMANTIC",
      "properties": {
        "type": "calls",
        "category": "SEMANTIC",
        "line_number": 42
      }
    }
  ],
  "meta": {
    "nodeTypeStats": {"function": 1},
    "edgeTypeStats": {"calls": 1},
    "legend": {"calls": "SEMANTIC"},
    "edgeTypeLabels": {"calls": "调用关系"},
    "edgeCategoryLabels": {"SEMANTIC": "语义"},
    "queryInfo": {"queryType": "both", "pathDepth": 1, "resolvedPathDepth": 1, "methodFullName": "..."}
  }
}
```

### Node Detail

`POST /api/v1/node_detail`

Returns node detail with decompressed content and all raw properties.

**Request body**:

```json
{"node_id": "abc123"}
```

**Response**:

```json
{
  "id": "abc123",
  "name": "createOrder",
  "full_name": "com.macro.mall.controller.OrderController#createOrder(...)",
  "content": "decompressed source code",
  "raw_properties": {
    "node_type": "function",
    "file_path": "mall-admin/src/.../OrderController.java",
    "line_start": 50,
    "line_end": 55,
    "is_library": false,
    "visibility": "public",
    "...": "..."
  }
}
```

### Code Search — Semantic Search

`POST /api/v1/code_search`

Searches code nodes by natural language query with vector similarity.

**Request body**:

```json
{
  "query": "用户下单的入口在哪里",
  "top_k": 5,
  "strategy": "weighted",
  "include_context": true,
  "context_depth": 1
}
```

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `query` | yes | — | Natural language query |
| `top_k` | no | 5 | Number of results |
| `strategy` | no | `"weighted"` | `weighted` / `auto` / `flat` / `per_type` |
| `type_weights` | no | null | Custom type weights, e.g. `{"function": 1.0}` |
| `include_context` | no | true | Include upstream/downstream context |
| `context_depth` | no | 1 | Graph traversal depth for context |
| `repo_id` | no | null | Filter by project |
| `branch_name` | no | null | Filter by branch |

**Response**:

```json
{
  "query": "用户下单的入口在哪里",
  "results": [
    {
      "id": "abc123",
      "node_type": "function",
      "full_name": "com.macro.mall.portal.controller.OmsPortalOrderController#list(...)",
      "name": "list",
      "digest": "// Method signature digest...",
      "content": null,
      "score": 0.92,
      "is_library": false,
      "source_code_location": "mall-portal/src/.../OmsPortalOrderController.java:77-82",
      "context": {
        "upstream": [{"id": "...", "node_type": "class", "full_name": "...", "edge_type": "contains", "edge_properties": {}}],
        "downstream": [{"id": "...", "node_type": "function", "full_name": "...", "edge_type": "injection_calls", "edge_properties": {"line_number": 51}}]
      }
    }
  ],
  "meta": {
    "total_vector_hits": 15,
    "returned": 5,
    "type_distribution": {"function": 1, "comment": 4},
    "strategy_used": "weighted",
    "context_nodes_count": 2,
    "elapsed_ms": 120
  }
}
```

### Entry Points

`POST /api/v1/get_entry_points`

Returns all entry point methods (Controller endpoints, main methods, etc.).

**Request body**:

```json
{"repo_id": "mall", "branch_name": "master"}
```

Both fields optional, defaults to AppConfig values.

**Response**:

```json
{
  "entry_points": [
    {
      "id": "ep001",
      "node_type": "function",
      "full_name": "com.macro.mall.controller.OrderController#createOrder(...)",
      "name": "createOrder",
      "source_code_location": "mall-admin/src/.../OrderController.java:50-55"
    }
  ],
  "meta": {"total": 247, "returned": 247, "elapsed_ms": 50}
}
```

## Node Rules

Each node must contain:

- `id`
- `type`
- `label`
- `properties`

All node types now include `file_path` in their properties (populated during analyzer phase).

Unknown properties must remain inside `properties`.

## Edge Rules

Each edge must contain:

- `id`
- `source`
- `target`
- `type`
- `category`
- `properties`

Unknown edge types are valid. Frontend consumers must render them with fallback styles.

## Compatibility Notes

- `type` is the canonical edge identifier
- `properties.type` is duplicated for compatibility with older UI logic
- `category` is exposed both top-level and in `properties` for progressive migration
- `content` in code_search results is only populated for library nodes (`is_library=true`); project code relies on `source_code_location`
