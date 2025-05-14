package com.puti.code.server.controller;

import com.puti.code.app.ProjectHandler;
import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.puti.code.server.dto.MethodSearchRequest;
import com.puti.code.server.dto.NodeDetailRequest;
import com.puti.code.server.dto.NodeRequest;
import com.vesoft.nebula.client.graph.data.ResultSet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration" })
@TestPropertySource(locations = "file:../config/application.properties")
public class GraphApiControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    public static void setup() {
        Assumptions.assumeTrue(Boolean.getBoolean("runNebulaIntegrationTests"),
                "Set -DrunNebulaIntegrationTests=true to run Nebula-backed integration tests");

        File configDir = Paths.get(System.getProperty("user.dir")).getParent().resolve("config").toFile();
        System.setProperty("config.dir", configDir.getAbsolutePath());

        File testProjectDir = new File(System.getProperty("user.dir")).getParentFile();
        testProjectDir = new File(testProjectDir, "code-graph-app/src/test/resources/test-project");

        AppConfig appConfig = AppConfig.getInstance();
        appConfig.setProjectRootPath(testProjectDir.getAbsolutePath());
        appConfig.setProjectId("projectx");
        appConfig.setBranch("master");
        appConfig.setTargetPackage("com.example.demo");
        appConfig.setGlobalLibraryPath("");
        appConfig.setGraphVectorEnabled(false);
        appConfig.setChatApiKey("dummy-chat-key");
        appConfig.setChatUrl("http://localhost:8080");
        appConfig.setChatCompletionPath("/v1/chat/completions");
        appConfig.setEmbeddingApiKey("dummy-embedding-key");
        appConfig.setEmbeddingUrl("http://localhost:8080");

        ProjectHandler projectHandler = new ProjectHandler();
        projectHandler.handle();

        try {
            Thread.sleep(4000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for NebulaGraph index propagation", e);
        }
    }

    @Test
    public void testSearchMethods() {
        ResultSet resultSet = new NebulaGraphClient()
                .execute("MATCH (v:function) RETURN v.function.name, v.function.full_name LIMIT 20");
        assertTrue(resultSet != null && resultSet.isSucceeded());

        MethodSearchRequest request = new MethodSearchRequest();
        request.setMethodName("getUser");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/search", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getBody().get("data");
        assertNotNull(records);
        assertFalse(records.isEmpty(), "Should find at least one method named getUser");
    }

    @Test
    public void testGetNodes() {
        NodeRequest request = new NodeRequest();
        request.setMethodFullName("com.example.demo.controller.UserController#getUser(java.lang.Long)");
        request.setQueryType("upstream");
        request.setPathDepth(3);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/nodes", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("nodes"));
        assertTrue(response.getBody().containsKey("edges"));
    }

    @Test
    public void testGetNodeDetail() {
        MethodSearchRequest searchReq = new MethodSearchRequest();
        searchReq.setMethodName("getUserInfo");
        ResponseEntity<Map> searchRes = restTemplate.postForEntity("/api/v1/search", searchReq, Map.class);
        List<Map<String, Object>> records = (List<Map<String, Object>>) searchRes.getBody().get("data");

        assertFalse(records.isEmpty(), "Should find getUserInfo method to test detail API");
        String nodeId = (String) records.get(0).get("id");

        NodeDetailRequest detailReq = new NodeDetailRequest();
        detailReq.setNodeId(nodeId);

        ResponseEntity<Map> detailRes = restTemplate.postForEntity("/api/v1/node_detail", detailReq, Map.class);

        assertEquals(HttpStatus.OK, detailRes.getStatusCode());
        assertNotNull(detailRes.getBody());
        assertEquals("getUserInfo", detailRes.getBody().get("name"));
    }
}
