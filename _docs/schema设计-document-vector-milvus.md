# Milvus Schema 设计

## 集合名称

集合名称通过配置文件中的 `milvus.collection` 属性指定。

## 字段定义

1. **document_id** (主键)
    - 类型: VarChar(32)
    - 描述: 说明书的唯一标识符, 对应mysql中documentation表的id
    - 主键: 是
    - 自动生成: 否

2. **content** (标题)
    - 类型: VarChar(65535)
    - 描述: 说明书的摘要，用于生成稠密向量和稀疏向量

3. **document_type**
   - 类型: VarChar(128)
   - 描述: 说明书类型，一级链路说明书：document-chain-1，二级链路说明书：document-chain-2，三级链路说明书：document-chain-3, 模块说明书：document-flow

4. **text_dense**
    - 类型: FloatVector
    - 维度: 由配置文件中的 `milvus.dimension` 指定（默认为1536）
    - 描述: 节点内容的稠密向量表示

5. **text_sparse**
    - 类型: SparseFloatVector
    - 方法: 默认bm25
    - 描述: 节点内容的稀疏向量表示

6. **created_at**
    - 类型: Int64
    - 描述: 记录创建时间（毫秒时间戳）

7. **updated_at**
    - 类型: Int64
    - 描述: 记录更新时间（毫秒时间戳）

8. **repo_id**
    - 类型: VarChar(256)
    - 描述: 代码所属仓库的ID，用于区分不同仓库的向量数据

9. **branch_name**
    - 类型: VarChar(256)
    - 描述: 代码所属分支名称，用于区分不同分支的向量数据