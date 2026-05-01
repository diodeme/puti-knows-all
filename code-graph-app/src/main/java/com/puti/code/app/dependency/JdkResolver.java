package com.puti.code.app.dependency;

import com.puti.code.app.dependency.BuildToolDetector.BuildTool;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据项目构建文件自动检测所需 JDK 版本，并匹配已配置的 JDK 安装路径。
 *
 * 配置示例：dependency.jdk_paths=/usr/lib/jvm/java-8,/usr/lib/jvm/java-21
 * 自动从每个 JDK 的 release 文件检测版本号，然后根据项目的 pom.xml / build.gradle 匹配。
 */
@Slf4j
public class JdkResolver {

    private static final Pattern GRADLE_SOURCE_COMPAT = Pattern.compile(
            "sourceCompatibility\\s*[=\\s]\\s*['\"]?([\\d._]+)['\"]?");
    private static final Pattern GRADLE_TOOLCHAIN = Pattern.compile(
            "languageVersion\\s*=\\s*JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)");

    private final Map<Integer, String> jdkInstallations = new TreeMap<>();

    /**
     * @param jdkPaths 配置的 JDK 安装路径列表
     */
    public JdkResolver(List<String> jdkPaths) {
        if (jdkPaths == null) {
            return;
        }
        for (String path : jdkPaths) {
            String trimmed = path.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!Files.isDirectory(Path.of(trimmed))) {
                log.warn("JDK path does not exist: {}", trimmed);
                continue;
            }
            int version = detectJdkVersion(Path.of(trimmed));
            if (version > 0) {
                jdkInstallations.put(version, trimmed);
                log.info("Registered JDK {} at {}", version, trimmed);
            }
        }
    }

    public boolean hasInstallations() {
        return !jdkInstallations.isEmpty();
    }

    /**
     * 解析目标项目所需的 JAVA_HOME。
     * 1. 从构建文件检测所需的 Java 版本
     * 2. 精确匹配已注册的 JDK；若无精确匹配，取 >= 所需版本的最低版本
     * 3. 都不匹配则回退到当前 JAVA_HOME
     */
    public String resolveJavaHome(String projectRootPath, BuildTool buildTool) {
        int requiredVersion = detectRequiredVersion(projectRootPath, buildTool);

        if (requiredVersion <= 0) {
            log.info("Could not detect required Java version from project, using current JAVA_HOME");
            return currentJavaHome();
        }

        log.info("Project requires Java {}", requiredVersion);

        // 精确匹配
        String exact = jdkInstallations.get(requiredVersion);
        if (exact != null) {
            log.info("Using JDK {} at {}", requiredVersion, exact);
            return exact;
        }

        // 兼容匹配：取 >= requiredVersion 的最低版本
        for (Map.Entry<Integer, String> entry : jdkInstallations.entrySet()) {
            if (entry.getKey() >= requiredVersion) {
                log.info("No exact JDK {} match, using compatible JDK {} at {}",
                        requiredVersion, entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }

        log.warn("No compatible JDK >= {} found in configured installations, falling back to current JAVA_HOME", requiredVersion);
        return currentJavaHome();
    }

    private String currentJavaHome() {
        String javaHome = System.getProperty("java.home");
        // java.home 指向 jre 子目录时，向上取 JDK 根目录
        if (javaHome != null && (javaHome.endsWith("/jre") || javaHome.endsWith("\\jre"))) {
            return javaHome.substring(0, javaHome.length() - 4);
        }
        return javaHome;
    }

    /**
     * 从 JDK 安装目录的 release 文件检测版本号。
     * release 文件格式：JAVA_VERSION="1.8.0_432" 或 JAVA_VERSION="21.0.10"
     */
    int detectJdkVersion(Path jdkPath) {
        Path releaseFile = jdkPath.resolve("release");
        if (!Files.exists(releaseFile)) {
            log.warn("No release file found in {}", jdkPath);
            return -1;
        }
        try (BufferedReader reader = Files.newBufferedReader(releaseFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("JAVA_VERSION=")) {
                    String raw = line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
                    return normalizeJavaVersion(raw);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read release file: {}", releaseFile, e);
        }
        return -1;
    }

    /**
     * 从项目构建文件检测所需的 Java 版本。
     */
    public int detectRequiredVersion(String projectRootPath, BuildTool buildTool) {
        return switch (buildTool) {
            case MAVEN -> detectMavenJavaVersion(projectRootPath);
            case GRADLE -> detectGradleJavaVersion(projectRootPath);
            case UNKNOWN -> -1;
        };
    }

    /**
     * 解析 pom.xml 中的 Java 版本声明。
     * 按优先级检查：java.version → maven.compiler.source → maven.compiler.target
     */
    int detectMavenJavaVersion(String projectRootPath) {
        Path pomXml = Path.of(projectRootPath, "pom.xml");
        if (!Files.exists(pomXml)) {
            return -1;
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomXml.toFile());
            Element properties = getFirstElement(doc, "properties");
            if (properties != null) {
                for (String key : new String[]{"java.version", "maven.compiler.source", "maven.compiler.target"}) {
                    String value = getElementText(properties, key);
                    if (value != null && !value.isEmpty()) {
                        int version = normalizeJavaVersion(value);
                        log.info("Detected Java version {} from pom.xml property '{}={}'", version, key, value);
                        return version;
                    }
                }
            }

            // 检查 maven-compiler-plugin 的 source/target 配置
            NodeList sourceNodes = doc.getElementsByTagName("source");
            if (sourceNodes.getLength() > 0) {
                String value = sourceNodes.item(0).getTextContent().trim();
                int version = normalizeJavaVersion(value);
                log.info("Detected Java version {} from maven-compiler-plugin <source>", version);
                return version;
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.warn("Failed to parse pom.xml: {}", pomXml, e);
        }
        return -1;
    }

    /**
     * 解析 build.gradle / build.gradle.kts 中的 Java 版本声明。
     * 检查 sourceCompatibility 和 Java toolchain。
     */
    int detectGradleJavaVersion(String projectRootPath) {
        for (String name : new String[]{"build.gradle", "build.gradle.kts"}) {
            Path buildFile = Path.of(projectRootPath, name);
            if (!Files.exists(buildFile)) {
                continue;
            }
            try {
                String content = Files.readString(buildFile);

                // 检查 toolchain
                Matcher toolchainMatcher = GRADLE_TOOLCHAIN.matcher(content);
                if (toolchainMatcher.find()) {
                    int version = Integer.parseInt(toolchainMatcher.group(1));
                    log.info("Detected Java version {} from Gradle toolchain", version);
                    return version;
                }

                // 检查 sourceCompatibility
                Matcher compatMatcher = GRADLE_SOURCE_COMPAT.matcher(content);
                if (compatMatcher.find()) {
                    int version = normalizeJavaVersion(compatMatcher.group(1));
                    log.info("Detected Java version {} from Gradle sourceCompatibility", version);
                    return version;
                }
            } catch (IOException e) {
                log.warn("Failed to read build file: {}", buildFile, e);
            }
        }
        return -1;
    }

    /**
     * 版本号归一化："1.8" → 8, "1.8.0_432" → 8, "8" → 8, "21" → 21
     */
    int normalizeJavaVersion(String version) {
        if (version == null || version.isEmpty()) {
            return -1;
        }
        String[] parts = version.split("[._\\-]");
        try {
            if ("1".equals(parts[0]) && parts.length > 1) {
                return Integer.parseInt(parts[1]); // "1.8" → 8
            }
            return Integer.parseInt(parts[0]); // "8", "11", "17", "21"
        } catch (NumberFormatException e) {
            log.warn("Cannot parse Java version: {}", version);
            return -1;
        }
    }

    private Element getFirstElement(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }
}
