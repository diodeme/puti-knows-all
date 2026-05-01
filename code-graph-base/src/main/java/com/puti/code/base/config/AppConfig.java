package com.puti.code.base.config;

import com.puti.code.base.enums.ParseType;
import com.puti.code.base.enums.GraphStorageType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 应用配置类
 */
@Slf4j
@Data
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    // NebulaGraph配置
    private String nebulaHosts;
    private String nebulaUsername;
    private String nebulaPassword;
    private String nebulaSpace;
    private int nebulaConnectionPoolSize;
    private int nebulaTimeout;
    private String graphStorageType;

    // Milvus配置
    private String milvusUri;
    private String milvusUsername;
    private String milvusPassword;
    private String milvusCollection;
    private int milvusDimension;
    private boolean graphVectorEnabled;

    // OpenAI配置
    private String embeddingUrl;
    private String embeddingModel;
    private String embeddingApiKey;

    private String chatUrl;
    private String chatModel;
    private String chatApiKey;
    private String chatCompletionPath;

    private String globalLibraryPath;
    private String globalDecompileOutputPath;

    // 依赖自动解析配置
    private boolean dependencyAutoResolve;
    private int dependencyTimeoutMinutes;
    private List<String> dependencyJdkPaths;
    private String dependencyFilterMode;
    private List<String> dependencyExcludeGroups;
    private List<String> dependencyIncludeGroups;

    // 项目配置
    private String projectRootPath;
    private String projectId;
    private String branch;
    private String targetPackage;
    private List<String> projectDiscardRegxList;

    private ParseType parseType;

    // Data Lineage Configs
    private List<String> lineageEntityPatterns;
    private List<String> lineageMappingMethods;
    private List<String> lineageAccessorSetters;
    private List<String> lineageAccessorGetters;

    private AppConfig() {
        loadConfig();
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    private void loadConfig() {
        Properties properties = new Properties();

        // 1. 优先加载 classpath 默认兜底配置
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("properties/application.properties")) {
            if (input != null) {
                try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                    log.info("Loaded default configuration from classpath");
                }
            } else {
                log.warn("Unable to find default application.properties in classpath");
            }
        } catch (IOException e) {
            log.error("Failed to load default application.properties", e);
        }

        // 2. 尝试读取外部配置文件并覆盖 (优先级: config.dir -> ./config/ -> ./)
        String customDir = System.getProperty("config.dir", System.getenv("MALING_CONFIG_DIR"));
        List<Path> searchPaths = new ArrayList<>();
        if (StringUtils.isNotBlank(customDir)) {
            searchPaths.add(Paths.get(customDir, "application.properties"));
        }
        searchPaths.add(Paths.get(System.getProperty("user.dir"), "config", "application.properties"));
        searchPaths.add(Paths.get(System.getProperty("user.dir"), "application.properties"));

        boolean externalLoaded = false;
        for (Path path : searchPaths) {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                    log.info("Loaded external configuration from: {}", path.toAbsolutePath());
                    externalLoaded = true;
                    break;
                } catch (IOException e) {
                    log.warn("Failed to load external configuration from: {}", path, e);
                }
            }
        }
        
        if (!externalLoaded) {
            log.info("No external configuration found, running purely on defaults.");
        }

        try {
            // 加载NebulaGraph配置
            nebulaHosts = properties.getProperty("nebula.hosts");
            nebulaUsername = properties.getProperty("nebula.username");
            nebulaPassword = properties.getProperty("nebula.password");
            nebulaSpace = properties.getProperty("nebula.space");
            nebulaConnectionPoolSize = Integer.parseInt(properties.getProperty("nebula.connection_pool_size", "10"));
            nebulaTimeout = Integer.parseInt(properties.getProperty("nebula.timeout", "60000"));
            graphStorageType = properties.getProperty("graph.storage.type", GraphStorageType.NEBULA.getValue());

            // 加载Milvus配置
            milvusUri = properties.getProperty("milvus.uri");
            milvusUsername = properties.getProperty("milvus.username");
            milvusPassword = properties.getProperty("milvus.password");
            milvusCollection = properties.getProperty("milvus.collection");
            milvusDimension = Integer.parseInt(properties.getProperty("milvus.dimension", "1536"));
            graphVectorEnabled = Boolean.parseBoolean(properties.getProperty("graph.vector.enabled", "false"));

            // 加载OpenAI配置
            embeddingModel = properties.getProperty("embedding.model");
            embeddingApiKey = properties.getProperty("embedding.api_key");
            embeddingUrl = properties.getProperty("embedding.url");

            chatModel = properties.getProperty("chat.model");
            chatApiKey = properties.getProperty("chat.api_key");
            chatUrl = properties.getProperty("chat.url");
            chatCompletionPath = properties.getProperty("chat.completion_path");

            globalLibraryPath = properties.getProperty("global.library_path");
            globalDecompileOutputPath = properties.getProperty("global.decompile_output_path");

            dependencyAutoResolve = Boolean.parseBoolean(properties.getProperty("dependency.auto_resolve", "true"));
            dependencyTimeoutMinutes = Integer.parseInt(properties.getProperty("dependency.timeout_minutes", "30"));
            dependencyJdkPaths = parseListProperty(properties.getProperty("dependency.jdk_paths"), List.of());
            dependencyFilterMode = properties.getProperty("dependency.filter_mode", "blacklist");
            dependencyExcludeGroups = parseListProperty(properties.getProperty("dependency.exclude.groups"), List.of());
            dependencyIncludeGroups = parseListProperty(properties.getProperty("dependency.include.groups"), List.of());

            // 加载项目配置
            projectRootPath = properties.getProperty("project.root_path");
            if (projectRootPath == null || projectRootPath.isEmpty()) {
                // 如果未指定项目根路径，则使用当前路径
                projectRootPath = Paths.get("").toAbsolutePath().toString();
            }
            projectId = properties.getProperty("project.id");
            targetPackage = properties.getProperty("project.target.package");
            branch = properties.getProperty("project.branch");
            String property = properties.getProperty("project.discard.regx.list");
            projectDiscardRegxList = StringUtils.isBlank(property) ? new ArrayList<>() :
                    new ArrayList<>(List.of(property.split(",")));

            // Extract Data Lineage Configs
            lineageEntityPatterns = parseListProperty(properties.getProperty("lineage.entity.patterns"), List.of(".*DO$", ".*DTO$", ".*VO$", ".*Entity$", ".*PO$", ".*Model$"));
            lineageMappingMethods = parseListProperty(properties.getProperty("lineage.mapping.methods"), List.of(".*BeanUtil.*\\.copyProperties", ".*MapStruct.*"));
            lineageAccessorSetters = parseListProperty(properties.getProperty("lineage.accessor.setters"), List.of("^set[A-Z].*"));
            lineageAccessorGetters = parseListProperty(properties.getProperty("lineage.accessor.getters"), List.of("^get[A-Z].*", "^is[A-Z].*"));

            log.info("Configuration loaded successfully");
        } catch (NumberFormatException e) {
            log.error("Invalid number format in configuration", e);
        }
    }

    private List<String> parseListProperty(String value, List<String> defaultList) {
        if (StringUtils.isBlank(value)) {
            return defaultList;
        }
        return new ArrayList<>(List.of(value.split(",")));
    }

    /**
     * 获取相对于项目根路径的路径
     *
     * @param absolutePath 绝对路径
     * @return 相对路径
     */
    public String getRelativePath(String absolutePath) {
        Path pathAbsolute = Paths.get(absolutePath);
        Path pathBase = Paths.get(projectRootPath);
        return pathBase.relativize(pathAbsolute).toString().replace('\\', '/');
    }

}
