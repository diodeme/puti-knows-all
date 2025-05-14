# Server Protocol

## Graph Query Response

`POST /api/v1/nodes`

Returns:

```json
{
  "nodes": [
    {
      "id": "node-id",
      "type": "function",
      "label": "createUser",
      "properties": {
        "full_name": "com.demo.UserService#createUser()",
        "visibility": "public"
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
    "nodeTypeStats": {
      "function": 1
    },
    "edgeTypeStats": {
      "calls": 1
    },
    "legend": {
      "calls": "SEMANTIC"
    },
    "queryInfo": {
      "queryType": "OUT",
      "pathDepth": 2
    }
  }
}
```

## Node Rules

Each node must contain:

- `id`
- `type`
- `label`
- `properties`

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

## Detail Query

`POST /api/v1/node_detail`

Returns node detail plus `raw_properties`. Consumers should treat `raw_properties` as an open-ended object.

## Compatibility Notes

- `type` is the canonical edge identifier
- `properties.type` is duplicated for compatibility with older UI logic
- `category` is exposed both top-level and in `properties` for progressive migration
