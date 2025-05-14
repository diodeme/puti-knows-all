package com.puti.code.repository.milvus;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.gson.JsonObject;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.entity.vector.DocumentationVector;
import com.puti.code.base.util.Json;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 文档向量数据库客户端
 * 专门处理Documentation实体的向量化存储
 * 
 * @author diodehe
 */
@Slf4j
public class DocumentationVectorClient implements AutoCloseable {

    private final MilvusClientV2 client;
    private final AppConfig config;
    
    // 文档向量集合的字段定义
    private static final String DOCUMENT_ID_FIELD = "document_id";
    private static final String CONTENT_FIELD = "content";
    private static final String DOCUMENT_TYPE_FIELD = "document_type";
    private static final String TEXT_DENSE_FIELD = "text_dense";
    private static final String TEXT_SPARSE_FIELD = "text_sparse";
    private static final String CREATED_AT_FIELD = "created_at";
    private static final String UPDATED_AT_FIELD = "updated_at";
    private static final String REPO_ID_FIELD = "repo_id";
    private static final String BRANCH_NAME_FIELD = "branch_name";

    public DocumentationVectorClient() {
        config = AppConfig.getInstance();
        client = null;
//        client = new MilvusClientV2(ConnectConfig.builder()
//                .uri(config.getMilvusUri())
//                .username(config.getMilvusUsername())
//                .password(config.getMilvusPassword())
//                .build());
//        ensureDocumentationCollectionExists();
    }

    /**
     * 确保文档向量集合存在
     */
    private void ensureDocumentationCollectionExists() {
        String collectionName = getDocumentationCollectionName();

        // 检查集合是否存在
        HasCollectionReq hasCollectionReq = HasCollectionReq.builder()
                .collectionName(collectionName)
                .build();

        boolean exists = client.hasCollection(hasCollectionReq);

        if (exists) {
            // 加载集合
            LoadCollectionReq loadCollectionReq = LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();

            client.loadCollection(loadCollectionReq);
            log.info("Loaded existing documentation collection: {}", collectionName);
            return;
        }

        createDocumentationCollection(collectionName);
    }

    /**
     * 创建文档向量集合
     */
    private void createDocumentationCollection(String collectionName) {
        Map<String, Object> analyzerParams = new HashMap<>();
        analyzerParams.put("tokenizer", "jieba");
        analyzerParams.put("filter", Lists.newArrayList("lowercase", "asciifolding", "removepunct"));

        CreateCollectionReq.CollectionSchema schema = MilvusClientV2.CreateSchema();

        // 添加字段到schema
        schema.addField(AddFieldReq.builder()
                .fieldName(DOCUMENT_ID_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(32)
                .isPrimaryKey(true)
                .autoID(false)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(CONTENT_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .analyzerParams(analyzerParams)
                .enableAnalyzer(true)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(DOCUMENT_TYPE_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(128)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(TEXT_DENSE_FIELD)
                .dataType(DataType.FloatVector)
                .dimension(config.getMilvusDimension())
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(TEXT_SPARSE_FIELD)
                .dataType(DataType.SparseFloatVector)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(CREATED_AT_FIELD)
                .dataType(DataType.Int64)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(UPDATED_AT_FIELD)
                .dataType(DataType.Int64)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(REPO_ID_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(256)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(BRANCH_NAME_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(256)
                .build());

        // 添加BM25函数用于稀疏向量
        schema.addFunction(CreateCollectionReq.Function.builder()
                .functionType(FunctionType.BM25)
                .name("doc_bm25_emb")
                .inputFieldNames(Collections.singletonList(CONTENT_FIELD))
                .outputFieldNames(Collections.singletonList(TEXT_SPARSE_FIELD))
                .build());

        // 创建索引
        IndexParam indexParamForTextDense = IndexParam.builder()
                .fieldName(TEXT_DENSE_FIELD)
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.IP)
                .build();

        Map<String, Object> sparseParams = new HashMap<>();
        sparseParams.put("inverted_index_algo", "DAAT_MAXSCORE");
        IndexParam indexParamForTextSparse = IndexParam.builder()
                .fieldName(TEXT_SPARSE_FIELD)
                .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX)
                .metricType(IndexParam.MetricType.BM25)
                .extraParams(sparseParams)
                .build();

        List<IndexParam> indexParams = Arrays.asList(indexParamForTextDense, indexParamForTextSparse);

        CreateCollectionReq createCollectionReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build();

        client.createCollection(createCollectionReq);

        // 加载集合
        LoadCollectionReq loadCollectionParam = LoadCollectionReq.builder()
                .collectionName(collectionName)
                .build();

        client.loadCollection(loadCollectionParam);

        log.info("Created and loaded documentation collection: {}", collectionName);
    }

    /**
     * 获取文档集合名称
     */
    private String getDocumentationCollectionName() {
        return config.getMilvusCollection();
    }

    /**
     * 批量插入文档向量
     *
     * @param documentationVectors 文档列表
     */
    public void batchUpsertDocumentationVectors(List<DocumentationVector> documentationVectors) {
        if (documentationVectors == null || documentationVectors.isEmpty()) {
            log.warn("Documentation list is empty, skipping batch vector upsert");
            return;
        }

        try {
            List<JsonObject> data = new ArrayList<>();

            for (DocumentationVector documentationVector : documentationVectors) {
                if (documentationVector == null || documentationVector.getDocumentId() == null) {
                    log.warn("Skipping null documentationVector or documentationVector without ID");
                    continue;
                }

                // 创建向量数据
                JsonObject row = createDocumentationVectorData(documentationVector);
                data.add(row);
            }

            if (data.isEmpty()) {
                log.warn("No valid documentation data to upsert");
                return;
            }

            // 批量插入向量数据库
            UpsertReq upsertReq = UpsertReq.builder()
                    .collectionName(getDocumentationCollectionName())
                    .data(data)
                    .build();

            client.upsert(upsertReq);
            log.info("Successfully batch upserted {} documentation vectors", data.size());

        } catch (Exception e) {
            log.error("Failed to batch upsert documentation vectors", e);
        }
    }

    /**
     * 创建文档向量数据
     */
    private JsonObject createDocumentationVectorData(DocumentationVector documentationVector) {
        long currentTimeMillis = System.currentTimeMillis();

        JsonObject row = new JsonObject();
        row.addProperty(DOCUMENT_ID_FIELD, documentationVector.getDocumentId());
        row.add(TEXT_DENSE_FIELD, Json.toJsonTree(Floats.asList(documentationVector.getTextDense())));

        // 处理摘要字段，限制长度
        String content = documentationVector.getContent();
        if (StringUtils.isNotBlank(content)) {
            content = StringUtils.substring(content, 0, 65535);
        }
        row.addProperty(CONTENT_FIELD, StringUtils.defaultString(content, ""));

        row.addProperty(DOCUMENT_TYPE_FIELD, documentationVector.getDocumentType().getValue());
        row.addProperty(CREATED_AT_FIELD, currentTimeMillis);
        row.addProperty(UPDATED_AT_FIELD, currentTimeMillis);
        row.addProperty(REPO_ID_FIELD, documentationVector.getProjectId());
        row.addProperty(BRANCH_NAME_FIELD, documentationVector.getBranchName());

        return row;
    }

    @Override
    public void close() {
        if (client != null) {
            client.flush(FlushReq.builder()
                    .collectionNames(Lists.newArrayList(getDocumentationCollectionName()))
                    .build());
            client.close();
            log.info("Documentation vector client closed");
        }
    }
}
