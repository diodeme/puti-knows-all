package com.puti.code.app.query;

import com.puti.code.base.config.AppConfig;
import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 简单的Nebula查询运行器
 * 通过main函数直接执行nGQL查询
 */
@Slf4j
public class SimpleNebulaQueryRunner implements AutoCloseable {

    private final NebulaPool pool;
    private Session session;
    private final AppConfig config;

    /**
     * 构造函数
     * 初始化连接池和会话
     */
    public SimpleNebulaQueryRunner() {
        this.config = AppConfig.getInstance();
        this.pool = new NebulaPool();
        initPool();
    }

    /**
     * 初始化连接池
     */
    private void initPool() {
        try {
            NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
            nebulaPoolConfig.setMaxConnSize(config.getNebulaConnectionPoolSize());
            nebulaPoolConfig.setTimeout(config.getNebulaTimeout());

            List<HostAddress> addresses = parseHostAddresses(config.getNebulaHosts());
            pool.init(addresses, nebulaPoolConfig);

            session = pool.getSession(config.getNebulaUsername(), config.getNebulaPassword(), false);
            session.execute("USE " + config.getNebulaSpace());

            log.info("Connected to NebulaGraph successfully. Hosts: {}, Space: {}",
                    config.getNebulaHosts(), config.getNebulaSpace());
        } catch (Exception e) {
            log.error("Failed to initialize NebulaGraph connection", e);
            throw new RuntimeException("Failed to initialize NebulaGraph connection", e);
        }
    }

    /**
     * 解析主机地址字符串
     *
     * @param hosts 主机地址字符串，格式为 "host1:port1,host2:port2"
     * @return HostAddress列表
     */
    private List<HostAddress> parseHostAddresses(String hosts) {
        List<HostAddress> addresses = new ArrayList<>();
        if (hosts == null || hosts.isEmpty()) {
            return addresses;
        }

        for (String host : hosts.split(",")) {
            String[] parts = host.trim().split(":");
            if (parts.length == 2) {
                try {
                    addresses.add(new HostAddress(parts[0], Integer.parseInt(parts[1])));
                } catch (NumberFormatException e) {
                    log.warn("Invalid port number in host: {}", host);
                }
            }
        }
        return addresses;
    }

    /**
     * 执行nGQL查询
     *
     * @param nGQL nGQL查询语句
     * @return NebulaQueryResult查询结果
     */
    public NebulaQueryResult execute(String nGQL) {
        try {
            log.debug("Executing nGQL: {}", nGQL);
            ResultSet resultSet = session.execute(nGQL);
            return NebulaQueryResult.fromResultSet(resultSet);
        } catch (IOErrorException e) {
            log.error("Failed to execute nGQL: {}", nGQL, e);
            throw new RuntimeException("Failed to execute nGQL: " + nGQL, e);
        }
    }

    /**
     * 执行nGQL查询并返回格式化结果
     * 返回 AI 友好的 JSON 格式，不包含十六进制
     *
     * @param nGQL nGQL查询语句
     * @return 格式化后的结果（JSON兼容的Map）
     */
    public Map<String, Object> executeFormatted(String nGQL) {
        try {
            log.debug("Executing nGQL: {}", nGQL);
            ResultSet resultSet = session.execute(nGQL);
            return NebulaResultFormatter.formatResultSet(resultSet);
        } catch (IOErrorException e) {
            log.error("Failed to execute nGQL: {}", nGQL, e);
            throw new RuntimeException("Failed to execute nGQL: " + nGQL, e);
        }
    }

    /**
     * 执行nGQL查询并打印结果（AI友好格式）
     *
     * @param nGQL nGQL查询语句
     */
    private void executeAndPrint(String nGQL) {
        System.out.println("\n=== Executing nGQL ===");
        System.out.println("Query: " + nGQL);

        Map<String, Object> result = executeFormatted(nGQL);
        System.out.println("\n=== Query Result (AI-Friendly Format) ===");
        System.out.println("Succeeded: " + result.get("succeeded"));
        System.out.println("Columns: " + result.get("columns"));
        System.out.println("RowCount: " + result.get("rowCount"));

        if (!(Boolean) result.get("succeeded")) {
            System.out.println("Error: " + result.get("errorMessage"));
        } else {
            System.out.println("Data: " + NebulaResultFormatter.toJsonString(result));
        }
    }

    /**
     * 释放资源
     */
    @Override
    public void close() {
        try {
            if (session != null) {
                session.release();
                log.info("Session released");
            }
            if (pool != null) {
                pool.close();
                log.info("Connection pool closed");
            }
        } catch (Exception e) {
            log.error("Failed to close NebulaGraph resources", e);
        }
    }

    /**
     * 主方法 - 通过命令行参数执行nGQL查询
     *
     * @param args 命令行参数，第一个参数为nGQL查询语句
     *             用法: java SimpleNebulaQueryRunner "nGQL查询语句"
     *             示例: java SimpleNebulaQueryRunner "SHOW TAGS"
     *                   java SimpleNebulaQueryRunner "MATCH (n:function) WHERE n.function.is_entry_point == true RETURN count(n) AS entry_count"
     */
    public static void main(String[] args) {
        log.info("Starting SimpleNebulaQueryRunner...");

        // 如果没有参数，显示使用帮助
        if (args.length == 0) {
            printUsage();
            // 默认查询: 展示entry_point节点的下游调用关系
            System.out.println("\n[自动执行查询: entry_point节点的所有下游调用]");
            try (SimpleNebulaQueryRunner runner = new SimpleNebulaQueryRunner()) {
                // 找一个有calls出边的entry_point节点
                System.out.println("\n--- 查找有下游的entry_point节点 ---");
                runner.executeAndPrint(
                    "MATCH (n:function)-[e:calls]->(m) WHERE n.function.is_entry_point == true " +
                    "RETURN id(n) AS vid, n.function.name AS name, count(e) AS call_count LIMIT 5"
                );

                // 直接下游调用 (1跳)
                System.out.println("\n--- 直接调用的方法 (1跳) ---");
                runner.executeAndPrint(
                    "MATCH (n:function)-[e:calls]->(m:function) WHERE n.function.is_entry_point == true " +
                    "RETURN n.function.name AS src, e.line_number AS line, m.function.name AS dst LIMIT 15"
                );

                // 2跳下游 - 间接调用的方法
                System.out.println("\n--- 间接调用的方法 (2跳) ---");
                runner.executeAndPrint(
                    "MATCH (n:function)-[e1:calls]->(m:function)-[e2:calls]->(p:function) " +
                    "WHERE n.function.is_entry_point == true " +
                    "RETURN n.function.name AS src, m.function.name AS level1, p.function.name AS level2 LIMIT 15"
                );

                // 获取子图 - 3跳
                System.out.println("\n--- 完整子图 (3跳) ---");
                runner.executeAndPrint(
                    "GET SUBGRAPH WITH PROP 3 STEPS FROM \"02c595bef8b791ddc1cd2c90cac505\" OUT calls YIELD VERTICES AS nodes, EDGES AS links"
                );
            }
            return;
        }

        // 第一个参数作为nGQL查询语句或文件路径
        String nGQL = args[0];

        // 检测是否为文件路径
        File potentialFile = new File(nGQL);
        if (potentialFile.exists() && potentialFile.isFile()) {
            // 作为文件读取
            try {
                Path filePath = potentialFile.toPath();
                nGQL = Files.readString(filePath).trim();
                log.info("Read nGQL from file: {}", potentialFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error: Failed to read file: " + potentialFile.getAbsolutePath());
                log.error("Failed to read nGQL file", e);
                return;
            }
        } else {
            // 修复 gradle args 引号转义问题
            nGQL = unescapeQuotes(nGQL);
        }

        try (SimpleNebulaQueryRunner runner = new SimpleNebulaQueryRunner()) {
            System.out.println("\n=== Executing nGQL ===");
            System.out.println("Query: " + nGQL);

            Map<String, Object> result = runner.executeFormatted(nGQL);
            System.out.println("\n=== Query Result (AI-Friendly Format) ===");
            System.out.println("Succeeded: " + result.get("succeeded"));
            System.out.println("Columns: " + result.get("columns"));
            System.out.println("RowCount: " + result.get("rowCount"));

            if (!(Boolean) result.get("succeeded")) {
                System.out.println("Error: " + result.get("errorMessage"));
            } else {
                System.out.println("Data: " + NebulaResultFormatter.toJsonString(result));
            }

        } catch (Exception e) {
            log.error("Error executing queries", e);
            System.err.println("Error: " + e.getMessage());
        }

        log.info("SimpleNebulaQueryRunner finished.");
    }

    /**
     * 修复 gradle args 传递时引号被错误转义的问题
     * 处理完整的转义序列: \", \\", \\\", etc.
     *
     * @param input 原始输入字符串
     * @return 修复后的字符串
     */
    private static String unescapeQuotes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == '"') {
                    // \" 还原为 "
                    sb.append('"');
                    i += 2;
                } else if (next == '\\') {
                    // \\ 还原为 \\
                    sb.append("\\\\");
                    i += 2;
                } else {
                    // 其他反斜杠保留
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 打印使用帮助
     */
    private static void printUsage() {
        System.out.println("\n===========================================");
        System.out.println("SimpleNebulaQueryRunner - nGQL查询工具");
        System.out.println("===========================================");
        System.out.println("\n用法:");
        System.out.println("  方式1 - 直接传入nGQL语句:");
        System.out.println("    ./gradlew :code-graph-app:run -PmyMain=\"com.puti.code.app.query.SimpleNebulaQueryRunner\" --args=\"\\\"<nGQL查询语句>\\\"\"");
        System.out.println("\n  方式2 - 从文件读取nGQL（推荐）:");
        System.out.println("    ./gradlew :code-graph-app:run -PmyMain=\"com.puti.code.app.query.SimpleNebulaQueryRunner\" --args=\"<文件路径>\"");
        System.out.println("\n示例:");
        System.out.println("  # 方式1: 查看所有Tag");
        System.out.println("  ./gradlew :code-graph-app:run -PmyMain=\"com.puti.code.app.query.SimpleNebulaQueryRunner\" --args=\"\\\"SHOW TAGS\\\"\"");
        System.out.println("\n  # 方式1: 查看所有Edge");
        System.out.println("  ./gradlew :code-graph-app:run -PmyMain=\"com.puti.code.app.query.SimpleNebulaQueryRunner\" --args=\"\\\"SHOW EDGES\\\"\"");
        System.out.println("\n  # 方式2: 从文件执行查询（推荐，避免引号转义问题）");
        System.out.println("    # 创建查询文件");
        System.out.println("    echo 'SHOW TAGS' > /tmp/query.ngql");
        System.out.println("    # 执行查询");
        System.out.println("    ./gradlew :code-graph-app:run -PmyMain=\"com.puti.code.app.query.SimpleNebulaQueryRunner\" --args=\"/tmp/query.ngql\"");
        System.out.println("\n  # 方式2: 多行查询示例");
        System.out.println("    cat > /tmp/subgraph.ngql << 'EOF'");
        System.out.println("    GET SUBGRAPH WITH PROP 3 STEPS FROM \"02c595bef8b791ddc1cd2c90cac505\"");
        System.out.println("    OUT calls YIELD VERTICES AS nodes, EDGES AS links");
        System.out.println("    EOF");
        System.out.println("    ./gradlew :code-graph-app:run -PmyMain=\"com.puti.code.app.query.SimpleNebulaQueryRunner\" --args=\"/tmp/subgraph.ngql\"");
        System.out.println("\n注意:");
        System.out.println("  - 方式2（文件模式）支持多行查询，推荐使用");
        System.out.println("  - 在MATCH语句中，属性引用需要使用类型前缀，如 n.function.is_entry_point");
        System.out.println("===========================================\n");
    }
}
