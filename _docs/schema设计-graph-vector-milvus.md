# Milvus Schema 设计

## 集合名称
集合名称通过配置文件中的 `milvus.collection` 属性指定。

## 字段定义

1. **id** (主键)
   - 类型: VarChar(32)
   - 描述: 节点的唯一标识符
   - 主键: 是
   - 自动生成: 否

2. **text_dense**
   - 类型: FloatVector
   - 维度: 由配置文件中的 `milvus.dimension` 指定（默认为1536）
   - 描述: 节点内容的稠密向量表示

3. **text_sparse**
   - 类型: SparseFloatVector
   - 方法: 默认bm25
   - 描述: 节点内容的稀疏向量表示

4. **node_type**
   - 类型: VarChar(128)
   - 描述: 节点类型，如 "class", "function", "file", "comment", "annotations" 等

5. **full_name**
   - 类型: VarChar(4096)
   - 描述: 节点的全限定名，如类的完整包路径或方法的完整签名

6. **digest**
   - 类型: VarChar(65535)
   - 描述: 用于生成稠密向量的原始内容摘要
   
7. **content**
   - 类型: VarChar(65535)
   - 描述: 用于生成稀疏向量的原始内容

8. **created_at**
   - 类型: Int64
   - 描述: 记录创建时间（毫秒时间戳）

9. **updated_at**
   - 类型: Int64
   - 描述: 记录更新时间（毫秒时间戳）

10. **repo_id**
   - 类型: VarChar(256)
   - 描述: 代码所属仓库的ID，用于区分不同仓库的向量数据

11. **branch_name**
   - 类型: VarChar(256)
   - 描述: 代码所属分支名称，用于区分不同分支的向量数据

## 索引配置
- 索引类型: HNSW (Hierarchical Navigable Small World)
- 度量类型: COSINE (余弦相似度)
- 额外参数: `{"M": 16, "efConstruction": 200}`
  - M: 每个节点的最大连接数
  - efConstruction: 构建索引时的搜索宽度