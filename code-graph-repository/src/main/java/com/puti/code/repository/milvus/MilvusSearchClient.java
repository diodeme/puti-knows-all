package com.puti.code.repository.milvus;

import com.puti.code.base.config.AppConfig;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量搜索客户端
 */
@Slf4j
public class MilvusSearchClient implements AutoCloseable {

    private final MilvusClientV2 client;
    private final AppConfig config;

    private static final String ID_FIELD = "id";
    private static final String NODE_TYPE_FIELD = "node_type";
    private static final String FULL_NAME_FIELD = "full_name";
    private static final String DIGEST = "digest";

    private static final List<String> OUTPUT_FIELDS = List.of(
            ID_FIELD, NODE_TYPE_FIELD, FULL_NAME_FIELD, DIGEST
    );

    public MilvusSearchClient() {
        config = AppConfig.getInstance();
        client = new MilvusClientV2(ConnectConfig.builder()
                .uri(config.getMilvusUri())
                .username(config.getMilvusUsername())
                .password(config.getMilvusPassword())
                .build());
        log.info("MilvusSearchClient initialized, collection: {}", config.getMilvusCollection());
    }

    /**
     * 向量搜索
     *
     * @param queryVector   查询向量
     * @param candidateSize 候选集大小（建议 topK * 3）
     * @return 搜索结果列表
     */
    public List<MilvusSearchResult> search(float[] queryVector, int candidateSize) {
        String collectionName = config.getMilvusCollection();

        SearchReq searchReq = SearchReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .topK(candidateSize)
                .outputFields(OUTPUT_FIELDS)
                .searchParams(Map.of("anns_field", "text_dense"))
                .build();

        SearchResp searchResp = client.search(searchReq);
        List<SearchResp.SearchResult> results = searchResp.getSearchResults().get(0);

        List<MilvusSearchResult> searchResults = new ArrayList<>();
        for (SearchResp.SearchResult result : results) {
            MilvusSearchResult sr = new MilvusSearchResult();
            sr.setId(String.valueOf(result.getEntity().get(ID_FIELD)));
            sr.setNodeType(String.valueOf(result.getEntity().get(NODE_TYPE_FIELD)));
            sr.setFullName(String.valueOf(result.getEntity().get(FULL_NAME_FIELD)));
            sr.setDigest(String.valueOf(result.getEntity().get(DIGEST)));
            sr.setScore(result.getScore());
            searchResults.add(sr);
        }

        log.info("Milvus search returned {} results (candidate size: {})", searchResults.size(), candidateSize);
        return searchResults;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            log.info("MilvusSearchClient closed");
        }
    }

    /**
     * Milvus 搜索结果
     */
    @lombok.Data
    public static class MilvusSearchResult {
        private String id;
        private String nodeType;
        private String fullName;
        private String digest;
        private float score;
    }
}
