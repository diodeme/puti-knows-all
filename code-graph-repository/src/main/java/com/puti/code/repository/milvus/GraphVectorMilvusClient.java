package com.puti.code.repository.milvus;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.gson.JsonObject;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.model.Node;
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
import io.milvus.v2.service.vector.request.RunAnalyzerReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.RunAnalyzerResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Milvus客户端
 */
@Slf4j
public class GraphVectorMilvusClient implements AutoCloseable {

    private final MilvusClientV2 client;
    private final AppConfig config;
    private static final String ID_FIELD = "id";
    private static final String TEXT_DENSE = "text_dense";
    private static final String TEXT_SPARSE = "text_sparse";
    private static final String NODE_TYPE_FIELD = "node_type";
    private static final String FULL_NAME_FIELD = "full_name";
    private static final String DIGEST = "digest";
    private static final String CONTENT = "content";
    private static final String CREATED_AT_FIELD = "created_at";
    private static final String UPDATED_AT_FIELD = "updated_at";
    private static final String REPO_ID_FIELD = "repo_id";
    private static final String BRANCH_NAME_FIELD = "branch_name";

    public GraphVectorMilvusClient() {
        config = AppConfig.getInstance();
        client = new MilvusClientV2(ConnectConfig.builder()
                .uri(config.getMilvusUri())
                .username(config.getMilvusUsername())
                .password(config.getMilvusPassword())
                .build());
        ensureCollectionExists();
    }

    /**
     * 确保集合存在
     */
    private void ensureCollectionExists() {
        String collectionName = config.getMilvusCollection();

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

            log.info("Loaded existing Milvus collection: {}", collectionName);
            return;
        }

        Map<String, Object> analyzerParams = new HashMap<>();
        analyzerParams.put("tokenizer", "jieba");
        analyzerParams.put("filter", Lists.newArrayList("lowercase","asciifolding","removepunct"));

        CreateCollectionReq.CollectionSchema schema = MilvusClientV2.CreateSchema();

        // 添加字段到schema
        schema.addField(AddFieldReq.builder()
                .fieldName(ID_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(32)
                .isPrimaryKey(true)
                .autoID(false)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(TEXT_DENSE)
                .dataType(DataType.FloatVector)
                .dimension(config.getMilvusDimension())
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(TEXT_SPARSE)
                .dataType(DataType.SparseFloatVector)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(NODE_TYPE_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(128)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(FULL_NAME_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(4096)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(DIGEST)
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName(CONTENT)
                .dataType(DataType.VarChar)
                .analyzerParams(analyzerParams)
                .maxLength(65535)
                .enableAnalyzer(true)
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

        schema.addFunction(CreateCollectionReq.Function.builder()
                .functionType(FunctionType.BM25)
                .name("text_bm25_emb")
                .inputFieldNames(Collections.singletonList(CONTENT))
                .outputFieldNames(Collections.singletonList(TEXT_SPARSE))
                .build());

        IndexParam indexParamForTextDense = IndexParam.builder()
                .fieldName(TEXT_DENSE)
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.IP)
                .build();

        Map<String, Object> sparseParams = new HashMap<>();
        sparseParams.put("inverted_index_algo", "DAAT_MAXSCORE");
        IndexParam indexParamForTextSparse = IndexParam.builder()
                .fieldName(TEXT_SPARSE)
                .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX)
                .metricType(IndexParam.MetricType.BM25)
                .extraParams(sparseParams)
                .build();
        List<IndexParam> indexParams = new ArrayList<>();
        indexParams.add(indexParamForTextDense);
        indexParams.add(indexParamForTextSparse);

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

        log.info("Created and loaded Milvus collection: {}", collectionName);
    }

    /**
     * 批量插入节点向量
     *
     * @param nodes 节点列表
     */
    public void batchUpsertNodeVectors(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        String collectionName = config.getMilvusCollection();

        // 获取当前时间的毫秒数
        long currentTimeMillis = System.currentTimeMillis();

        // 为每个节点创建JsonObject
        List<JsonObject> data = new ArrayList<>();
        for (Node node : nodes) {
            JsonObject row = new JsonObject();
            row.addProperty(ID_FIELD, node.getId());
            row.add(TEXT_DENSE, Json.toJsonTree(Floats.asList(node.getVector())));
            row.addProperty(CONTENT, node.needRecordOriContent() ?
                    StringUtils.substring(node.getOriContent(), 0, 65535) : "");
            row.addProperty(NODE_TYPE_FIELD, node.getNodeType().getValue());
            row.addProperty(FULL_NAME_FIELD, node.getFullName());
            row.addProperty(DIGEST, node.getVectorOriContent());
            row.addProperty(CREATED_AT_FIELD, currentTimeMillis);
            row.addProperty(UPDATED_AT_FIELD, currentTimeMillis);
            row.addProperty(REPO_ID_FIELD, config.getProjectId());
            row.addProperty(BRANCH_NAME_FIELD, config.getBranch());

            data.add(row);
        }

        UpsertReq upsertReq = UpsertReq.builder()
                .collectionName(collectionName)
                .data(data)
                .build();

        try {
            client.upsert(upsertReq);
            log.info("Batch upserted {} node vectors", nodes.size());
        } catch (Exception e) {
            log.error("Batch upsert node vectors error!", e);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.flush(FlushReq.builder().collectionNames(Lists.newArrayList(config.getMilvusCollection())).build());
            client.close();
            log.info("Milvus client closed");
        }
    }

    public static void main(String[] args) {
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .build());
        List<String> texts = new ArrayList<>();
        texts.add("""
                @ApiOperation("登录以后返回token")
                                             @RequestMapping(value = "/login", method = RequestMethod.POST)
                                             @ResponseBody
                                             public CommonResult login(@Validated
                                             @RequestBody
                                             UmsAdminLoginParam umsAdminLoginParam) {
                                                 String token = adminService.login(umsAdminLoginParam.getUsername(), umsAdminLoginParam.getPassword());
                                                 if (token == null) {
                                                     return CommonResult.validateFailed("用户名或密码错误");
                                                 }
                                                 Map<String, String> tokenMap = new HashMap<>();
                                                 tokenMap.put("token", token);
                                                 tokenMap.put("tokenHead", tokenHead);
                                                 return CommonResult.success(tokenMap);
                                             }
                """);

        Map<String, Object> analyzerParams = new HashMap<>();
        analyzerParams.put("tokenizer", "jieba");
        analyzerParams.put("filter", Lists.newArrayList("lowercase","asciifolding","removepunct"));
        RunAnalyzerResp resp = client.runAnalyzer(RunAnalyzerReq.builder()
                .texts(texts)
                .analyzerParams(analyzerParams)
                .build());
        List<RunAnalyzerResp.AnalyzerResult> results = resp.getResults();
        results.forEach(result -> {
            System.out.println(result.getTokens().stream().map(RunAnalyzerResp.AnalyzerToken::getToken).toList());
        });
    }
}
