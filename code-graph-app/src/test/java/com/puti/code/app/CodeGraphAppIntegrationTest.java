package com.puti.code.app;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.vesoft.nebula.client.graph.data.ResultSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.puti.code.base.util.IdGenerator;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试：专门用于验证代码图谱解析引擎正确地落库点与边
 */
public class CodeGraphAppIntegrationTest {

    private static NebulaGraphClient client;

    @BeforeAll
    public static void setup() {
        Assumptions.assumeTrue(Boolean.getBoolean("runNebulaIntegrationTests"),
                "Set -DrunNebulaIntegrationTests=true to run Nebula-backed integration tests");

        // 0. Ensure AppConfig picks up the real configuration from the root directory
        System.setProperty("config.dir", Paths.get("..", "config").toAbsolutePath().toString());

        // 1. 获取全局配置，模拟单测场景参数
        AppConfig config = AppConfig.getInstance();
        var testProjectPath = Paths.get("src/test/resources/test-project").toAbsolutePath().normalize();
        config.setProjectRootPath(testProjectPath.toString());
        config.setProjectId("demo-test");
        config.setTargetPackage("com.example.demo");
        config.setGlobalLibraryPath("");
        config.setGraphVectorEnabled(false);

        // 2. 跑真实的解析器全量链路，将节点和边刷入 Nebula
        System.out.println("Starting Graph Parsing for test project...");
        ProjectHandler handler = new ProjectHandler();
        handler.handle();
        System.out.println("Graph Parsing completed.");

        // 3. 准备 Nebula 客户端用于查库断言
        client = new NebulaGraphClient();
        
        // Ensure space is switched before querying
        client.execute("USE " + config.getNebulaSpace());
    }

    @AfterAll
    public static void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testNodesExist() {
        // 验证 Controller, Service, DAO 是否成功落库
        assertNodeExists("com.example.demo.controller.UserController");
        assertNodeExists("com.example.demo.service.UserService");
        assertNodeExists("com.example.demo.dao.UserDAO");
        assertNodeExists("com.example.demo.entity.UserDO");
        assertNodeExists("com.example.demo.dto.UserDTO");

        // 验证 file 节点
        String fileQuery = "MATCH (f:file) WHERE f.file.name == 'UserController.java' RETURN count(f) as c";
        ResultSet rs = client.execute(fileQuery);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Node for file UserController.java should exist");
    }

    @Test
    public void testReadsFieldEdge() {
        String expectedDstId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("com.example.demo.entity.UserDO#id")
                .isShadow(false).build());

        // 验证 UserDAO.findUserById 读取了 UserDO 的 setId 写入逻辑... 这里找 reads_field 边
        String query = "MATCH (src:function)-[e:reads_field]->(dst) " +
                "WHERE src.function.full_name CONTAINS 'UserDAO#saveUser' AND id(dst) == '" + expectedDstId + "' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        if (!rs.isSucceeded()) {
            throw new RuntimeException("query failed: " + rs.getErrorMessage());
        }
        long count = 0;
        if (rs.getRows().size() > 0) {
            count = rs.rowValues(0).values().get(0).asLong();
        }
        assertTrue(count > 0, "Expected a reads_field edge from saveUser to getId but found none");
    }

    @Test
    public void testPassesToEdge() {
        // 验证 UserController.registerUser 传参给了 UserService.createUser (passes_to)
        String query = "MATCH (src:function)-[e:passes_to]->(dst:function) " +
                "WHERE src.function.full_name CONTAINS 'UserController#registerUser' " + //
                "AND (dst.function.full_name CONTAINS 'UserService#createUser' OR dst.function.full_name CONTAINS 'IUserService#createUser') " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a passes_to edge between UserController and UserService");
    }

    @Test
    public void testInheritanceEdge() {
        // 验证 UserService extends BaseUserService
        String query = "MATCH (src:class)-[e:depends_on]->(dst:class) " +
                "WHERE src.class.full_name == 'com.example.demo.service.UserService' " +
                "AND dst.class.full_name == 'com.example.demo.service.BaseUserService' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a depends_on (inheritance) edge from UserService to BaseUserService");
    }

    @Test
    public void testImplementationEdge() {
        // 验证 UserService implements IUserService
        String query = "MATCH (src:class)-[e:depends_on]->(dst:class) " +
                "WHERE src.class.full_name == 'com.example.demo.service.UserService' " +
                "AND dst.class.full_name == 'com.example.demo.service.IUserService' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a depends_on (implementation) edge from UserService to IUserService");
    }

    @Test
    public void testSpringAnnotationEdge() {
        // 验证 UserService 被 @Service 标记
        // 对于 Spring 注解，DependencyInjectionProcessor 使用的是全限定名来拼接，如: 
        // element.getQualifiedName() + "#" + annotationName (全名)
        String expectedDstId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("com.example.demo.service.UserService#org.springframework.stereotype.Service")
                .isShadow(false).build());

        String query = "MATCH (src:class)-[e:contains]->(dst) " +
                "WHERE src.class.full_name == 'com.example.demo.service.UserService' " +
                "AND id(dst) == '" + expectedDstId + "' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = 0;
        if (rs != null && rs.isSucceeded() && rs.getRows() != null && !rs.getRows().isEmpty()) {
            count = rs.rowValues(0).values().get(0).asLong();
        }
        assertTrue(count > 0, "Expected a contains edge from UserService to @Service annotation");
    }

    @Test
    public void testInjectionCallsEdge() {
        // 验证 UserController 通过依赖注入调用到 UserService
        String query = "MATCH (src:function)-[e:injection_calls]->(dst:function) " +
                "WHERE src.function.full_name CONTAINS 'UserController#registerUser' " +
                "AND dst.function.full_name CONTAINS 'UserService#createUser' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected an injection_calls edge from UserController to UserService implementation");
    }

    @Test
    public void testMapsToEdge() {
        // 验证 UserService 里面的 getUserInfo 通过 copyProperties 触发了 maps_to
        String query = "MATCH (src:class)-[e:maps_to]->(dst:class) " +
                "WHERE src.class.full_name == 'com.example.demo.entity.UserDO' " +
                "AND dst.class.full_name == 'com.example.demo.dto.UserDTO' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        
        // Note: Our MAPS_TO edge is between the classes UserDO -> UserDTO
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a maps_to edge from UserDO to UserDTO");
    }

    @Test
    public void testFileContainsClassEdge() {
        // 验证 file -> contains -> class
        String query = "MATCH (src:file)-[e:contains]->(dst:class) " +
                "WHERE src.file.name == 'UserController.java' " +
                "AND dst.class.full_name == 'com.example.demo.controller.UserController' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a contains edge from file to class");
    }

    @Test
    public void testClassContainsFunctionEdge() {
        // 验证 class -> contains -> function
        String query = "MATCH (src:class)-[e:contains]->(dst:function) " +
                "WHERE src.class.full_name == 'com.example.demo.controller.UserController' " +
                "AND dst.function.name == 'getUser' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a contains edge from class to function");
    }

    @Test
    public void testWritesFieldEdge() {
        String expectedDstId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("com.example.demo.entity.UserDO#id")
                .isShadow(false).build());

        // 验证 UserDAO.findUserById 写入了 UserDO (writes_field)
        String query = "MATCH (src:function)-[e:writes_field]->(dst) " +
                "WHERE src.function.full_name CONTAINS 'UserDAO#findUserById' AND id(dst) == '" + expectedDstId + "' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a writes_field edge from findUserById to id field");
    }

    @Test
    public void testDocumentedByEdge() {
        // 验证 class -> documented_by -> comment
        String query = "MATCH (src:class)-[e:documented_by]->(dst:comment) " +
                "WHERE src.class.full_name == 'com.example.demo.controller.UserController' " +
                "RETURN count(e) as c";
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Expected a documented_by edge from UserController to its Javadoc");
    }

    @Test
    public void testInstanceOfEdge() {
        String expectedSrcId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("com.example.demo.controller.UserController#RestController")
                .isShadow(false).build());

        String expectedDstId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("org.springframework.web.bind.annotation.RestController")
                .isShadow(false).build());

        String query = "GO FROM \"" + expectedSrcId + "\" OVER instance_of YIELD dst(edge) as dstId";
        ResultSet rs = client.execute(query);
        boolean matched = false;
        if (rs != null && rs.isSucceeded() && rs.getRows() != null) {
            for (int i = 0; i < rs.getRows().size(); i++) {
                String actualDstId = rs.rowValues(i).values().get(0).toString().replace("\"", "");
                if (expectedDstId.equals(actualDstId)) {
                    matched = true;
                    break;
                }
            }
        }

        if (!matched) {
            System.out.println("DEBUG testInstanceOfEdge FAILED. Dumping nodes:");
            ResultSet dbg = client.execute("GO FROM \"" + expectedSrcId + "\" OVER instance_of YIELD dst(edge) as dstId");
            System.out.println("Any instance_of edges: " + dbg);
            System.out.println("EXPECTED SRC ID: " + expectedSrcId);
            System.out.println("EXPECTED DST ID: " + expectedDstId);
        }

        assertTrue(matched, "Expected an instance_of edge from @RestController annotation to its declaration");
    }

    private void assertNodeExists(String classFullName) {
        String query = String.format("MATCH (v:class) WHERE v.class.full_name == '%s' RETURN count(v) as c", classFullName);
        ResultSet rs = client.execute(query);
        long count = rs.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, "Node for class " + classFullName + " should exist in NebulaGraph");
    }
}


