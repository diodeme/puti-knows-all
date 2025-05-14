package com.puti.code.app;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.vesoft.nebula.client.graph.data.ResultSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 Spring 项目集成测试：验证接口注入和调用重定向是否能落到真实 Bean 实现。
 */
class SpringDemoDependencyInjectionIntegrationTest {

    private static NebulaGraphClient client;

    @BeforeAll
    static void setup() throws InterruptedException {
        Assumptions.assumeTrue(Boolean.getBoolean("runNebulaIntegrationTests"),
                "Set -DrunNebulaIntegrationTests=true to run Nebula-backed integration tests");

        System.setProperty("config.dir", Paths.get("..", "config").toAbsolutePath().normalize().toString());

        AppConfig config = AppConfig.getInstance();
        var projectPath = Paths.get("..", "external", "spring-demo").toAbsolutePath().normalize();
        Assumptions.assumeTrue(projectPath.toFile().exists(),
                "external/spring-demo is required for Spring demo integration tests");
        config.setProjectRootPath(projectPath.toString());
        config.setProjectId("spring-demo-di-test");
        config.setBranch("master");
        config.setTargetPackage("com.diode.kuibu");
        config.setGlobalLibraryPath("");
        config.setGraphVectorEnabled(false);

        ProjectHandler handler = new ProjectHandler();
        handler.handle();

        Thread.sleep(3000L);

        client = new NebulaGraphClient();
        client.execute("USE " + config.getNebulaSpace());
    }

    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void shouldBindAutowiredInterfaceFieldToConcreteBean() {
        String query = "MATCH (src:field)-[e:instance_of]->(dst:class) "
                + "WHERE src.field.full_name == 'com.diode.kuibu.controller.UserController#userService' "
                + "AND dst.class.full_name == 'com.diode.kuibu.proxy.UserServiceImpl' "
                + "RETURN count(e) as c";
        assertCountPositive(query, "Expected UserController.userService to resolve to UserServiceImpl");
    }

    @Test
    void shouldRetargetInterfaceCallToConcreteBeanMethod() {
        String query = "MATCH (src:function)-[e:injection_calls]->(dst:function) "
                + "WHERE src.function.full_name CONTAINS 'com.diode.kuibu.controller.UserController#add' "
                + "AND dst.function.full_name CONTAINS 'com.diode.kuibu.proxy.UserServiceImpl#getUserInfo' "
                + "RETURN count(e) as c";
        assertCountPositive(query, "Expected UserController.add to injection-call UserServiceImpl#getUserInfo");
    }

    private void assertCountPositive(String query, String message) {
        ResultSet resultSet = client.execute(query);
        assertTrue(resultSet != null && resultSet.isSucceeded(), "Query failed: " + query);
        long count = resultSet.rowValues(0).values().get(0).asLong();
        assertTrue(count > 0, message + ", actual count=" + count);
    }
}


