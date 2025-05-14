package com.puti.code.rule.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.puti.code.base.model.EdgeCategory;
import com.puti.code.rule.engine.MutationRuleDefinition;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 规则配置管理
 */
@Slf4j
@Data
public class RuleConfiguration {
    
    private static final RuleConfiguration INSTANCE = new RuleConfiguration();
    
    /**
     * 入口点规则配置
     */
    private EntryPointRules entryPointRules;
    
    /**
     * 依赖注入规则配置（预留扩展）
     */
    private DependencyInjectionRules dependencyInjectionRules;
    
    /**
     * RPC规则配置（预留扩展）
     */
    private RpcRules rpcRules;
    
    private RuleConfiguration() {
        loadConfiguration();
    }
    
    public static RuleConfiguration getInstance() {
        return INSTANCE;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfiguration() {
        InputStream input = null;
        try {
            Path extPath = Paths.get(System.getProperty("user.dir"), "config", "rules.yml");
            if (Files.exists(extPath)) {
                input = Files.newInputStream(extPath);
                log.info("Loading rules from external config: {}", extPath.toAbsolutePath());
            } else {
                input = getClass().getClassLoader().getResourceAsStream("rules/rules.yml");
                if (input != null) {
                    log.info("Loading rules from classpath");
                }
            }
            
            if (input == null) {
                log.warn("Rules configuration file not found, using default rules");
                loadDefaultRules();
                return;
            }
            
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            
            // 加载入口点规则
            if (config.containsKey("entryPointRules")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entryPointConfig = (Map<String, Object>) config.get("entryPointRules");
                this.entryPointRules = parseEntryPointRules(entryPointConfig);
            }
            if (config.containsKey("dependencyInjectionRules")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dependencyConfig = (Map<String, Object>) config.get("dependencyInjectionRules");
                this.dependencyInjectionRules = parseDependencyInjectionRules(dependencyConfig);
            }
            
            log.info("Rules configuration loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load rules configuration, using default rules", e);
            loadDefaultRules();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    private DependencyInjectionRules parseDependencyInjectionRules(Map<String, Object> config) {
        DependencyInjectionRules rules = new DependencyInjectionRules();
        rules.setFactMapping(createDefaultDependencyInjectionFactMapping());
        rules.setResolutionRules(createDefaultDependencyInjectionResolutionRules());
        if (config.containsKey("enabled")) {
            rules.setEnabled((Boolean) config.get("enabled"));
        }
        if (config.containsKey("factMapping")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> factMapping = (Map<String, Object>) config.get("factMapping");
            rules.setFactMapping(parseDependencyInjectionFactMapping(factMapping));
        }
        if (config.containsKey("resolutionRules")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resolutionRules = (Map<String, Object>) config.get("resolutionRules");
            rules.setResolutionRules(parseDependencyInjectionResolutionRules(resolutionRules));
        }
        if (config.containsKey("customFunctions")) {
            @SuppressWarnings("unchecked")
            Map<String, String> customFunctions = (Map<String, String>) config.get("customFunctions");
            rules.setCustomFunctions(customFunctions);
        }
        if (config.containsKey("rules")) {
            List<?> rawRules = (List<?>) config.get("rules");
            rules.setRules(rawRules.stream().map(item -> {
                if (item instanceof String expression) {
                    DependencyInjectionRule rule = new DependencyInjectionRule();
                    rule.setName(expression);
                    rule.setWhen(expression);
                    return rule;
                }
                Map<String, Object> ruleMap = (Map<String, Object>) item;
                DependencyInjectionRule rule = new DependencyInjectionRule();
                rule.setName((String) ruleMap.get("name"));
                if (ruleMap.containsKey("match")) {
                    rule.setMatch((String) ruleMap.get("match"));
                }
                rule.setWhen((String) ruleMap.get("when"));
                if (ruleMap.containsKey("priority")) {
                    rule.setPriority(Integer.parseInt(String.valueOf(ruleMap.get("priority"))));
                }
                if (ruleMap.containsKey("emitProperties")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> emitProperties = (Map<String, Object>) ruleMap.get("emitProperties");
                    rule.setEmitProperties(emitProperties);
                }
                if (ruleMap.containsKey("emitProperty")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> emitProperties = (Map<String, Object>) ruleMap.get("emitProperty");
                    rule.setEmitProperties(emitProperties);
                }
                if (ruleMap.containsKey("emitEdge")) {
                    Object emitEdge = ruleMap.get("emitEdge");
                    if (emitEdge instanceof String edgeType) {
                        rule.setEdgeType(edgeType);
                    } else if (emitEdge instanceof Map<?, ?> edgeConfig) {
                        Object edgeType = edgeConfig.get("type");
                        if (edgeType != null) {
                            rule.setEdgeType(String.valueOf(edgeType));
                        }
                        Object edgeCategory = edgeConfig.get("category");
                        if (edgeCategory != null) {
                            rule.setEdgeCategory(EdgeCategory.valueOf(String.valueOf(edgeCategory)));
                        }
                    }
                }
                if (ruleMap.containsKey("edgeType")) {
                    rule.setEdgeType((String) ruleMap.get("edgeType"));
                }
                if (ruleMap.containsKey("edgeCategory")) {
                    rule.setEdgeCategory(EdgeCategory.valueOf(String.valueOf(ruleMap.get("edgeCategory"))));
                }
                return rule;
            }).toList());
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private DependencyInjectionFactMapping parseDependencyInjectionFactMapping(Map<String, Object> config) {
        DependencyInjectionFactMapping factMapping = createDefaultDependencyInjectionFactMapping();
        if (config.containsKey("providerAnnotations")) {
            factMapping.setProviderAnnotations(((List<Map<String, Object>>) config.get("providerAnnotations")).stream()
                    .map(this::parseProviderAnnotationMapping)
                    .toList());
        }
        if (config.containsKey("injectionAnnotations")) {
            factMapping.setInjectionAnnotations(((List<Map<String, Object>>) config.get("injectionAnnotations")).stream()
                    .map(this::parseInjectionAnnotationMapping)
                    .toList());
        }
        if (config.containsKey("qualifierAnnotations")) {
            factMapping.setQualifierAnnotations(((List<Map<String, Object>>) config.get("qualifierAnnotations")).stream()
                    .map(this::parseQualifierAnnotationMapping)
                    .toList());
        }
        if (config.containsKey("priorityAnnotations")) {
            factMapping.setPriorityAnnotations(((List<Map<String, Object>>) config.get("priorityAnnotations")).stream()
                    .map(this::parsePriorityAnnotationMapping)
                    .toList());
        }
        return factMapping;
    }

    @SuppressWarnings("unchecked")
    private DependencyInjectionResolutionRules parseDependencyInjectionResolutionRules(Map<String, Object> config) {
        DependencyInjectionResolutionRules rules = createDefaultDependencyInjectionResolutionRules();
        if (config.containsKey("order")) {
            rules.setOrder((List<String>) config.get("order"));
        }
        return rules;
    }

    private ProviderAnnotationMapping parseProviderAnnotationMapping(Map<String, Object> config) {
        ProviderAnnotationMapping mapping = new ProviderAnnotationMapping();
        mapping.setAnnotation(String.valueOf(config.get("annotation")));
        if (config.containsKey("target")) {
            mapping.setTarget(String.valueOf(config.get("target")));
        }
        if (config.containsKey("nameAttributes")) {
            @SuppressWarnings("unchecked")
            List<String> nameAttributes = (List<String>) config.get("nameAttributes");
            mapping.setNameAttributes(nameAttributes);
        }
        if (config.containsKey("fallbackNameSource")) {
            mapping.setFallbackNameSource(String.valueOf(config.get("fallbackNameSource")));
        } else {
            mapping.setFallbackNameSource(defaultProviderFallbackNameSource(mapping.getTarget()));
        }
        return mapping;
    }

    private InjectionAnnotationMapping parseInjectionAnnotationMapping(Map<String, Object> config) {
        InjectionAnnotationMapping mapping = new InjectionAnnotationMapping();
        mapping.setAnnotation(String.valueOf(config.get("annotation")));
        if (config.containsKey("mode")) {
            mapping.setMode(String.valueOf(config.get("mode")));
        }
        if (config.containsKey("nameAttribute")) {
            mapping.setNameAttribute(String.valueOf(config.get("nameAttribute")));
        }
        return mapping;
    }

    private QualifierAnnotationMapping parseQualifierAnnotationMapping(Map<String, Object> config) {
        QualifierAnnotationMapping mapping = new QualifierAnnotationMapping();
        mapping.setAnnotation(String.valueOf(config.get("annotation")));
        if (config.containsKey("attribute")) {
            mapping.setAttribute(String.valueOf(config.get("attribute")));
        }
        return mapping;
    }

    private PriorityAnnotationMapping parsePriorityAnnotationMapping(Map<String, Object> config) {
        PriorityAnnotationMapping mapping = new PriorityAnnotationMapping();
        mapping.setAnnotation(String.valueOf(config.get("annotation")));
        if (config.containsKey("priority")) {
            mapping.setPriority(Integer.parseInt(String.valueOf(config.get("priority"))));
        }
        return mapping;
    }
    
    /**
     * 解析入口点规则配置
     */
    @SuppressWarnings("unchecked")
    private EntryPointRules parseEntryPointRules(Map<String, Object> config) {
        EntryPointRules rules = new EntryPointRules();
        
        if (config.containsKey("enabled")) {
            rules.setEnabled((Boolean) config.get("enabled"));
        }
        
        if (config.containsKey("rules")) {
            rules.setRules((List<String>) config.get("rules"));
        }
        
        if (config.containsKey("customFunctions")) {
            rules.setCustomFunctions((Map<String, String>) config.get("customFunctions"));
        }
        
        return rules;
    }
    
    /**
     * 加载默认规则
     */
    private void loadDefaultRules() {
        this.entryPointRules = new EntryPointRules();
        this.entryPointRules.setEnabled(true);
        this.entryPointRules.setRules(List.of());
        this.dependencyInjectionRules = new DependencyInjectionRules();
        this.dependencyInjectionRules.setEnabled(true);
        this.dependencyInjectionRules.setFactMapping(createDefaultDependencyInjectionFactMapping());
        this.dependencyInjectionRules.setResolutionRules(createDefaultDependencyInjectionResolutionRules());
        this.dependencyInjectionRules.setCustomFunctions(Map.of());
        this.dependencyInjectionRules.setRules(List.of());
    }

    private DependencyInjectionRule createDependencyRule(String name, String expression, int priority) {
        DependencyInjectionRule rule = new DependencyInjectionRule();
        rule.setName(name);
        rule.setWhen(expression);
        rule.setEdgeType("instance_of");
        rule.setEdgeCategory(EdgeCategory.FRAMEWORK);
        rule.setPriority(priority);
        rule.setEmitProperties(Map.of(
                "injection_mode", "#injectionMode",
                "resolution_kind", "#resolutionKind",
                "resolved_type", "#resolvedTypeName",
                "requested_name", "#requestedName"));
        return rule;
    }

    private DependencyInjectionFactMapping createDefaultDependencyInjectionFactMapping() {
        return new DependencyInjectionFactMapping();
    }

    private DependencyInjectionResolutionRules createDefaultDependencyInjectionResolutionRules() {
        DependencyInjectionResolutionRules rules = new DependencyInjectionResolutionRules();
        rules.setOrder(List.of(
                "qualifier",
                "requestedName",
                "memberName",
                "requiredTypeDefaultName",
                "priority",
                "uniqueCandidate"));
        return rules;
    }

    private String defaultProviderFallbackNameSource(String target) {
        return "METHOD".equalsIgnoreCase(target) ? "methodName" : "typeSimpleNameLowerCamel";
    }
    
    /**
     * 入口点规则配置
     */
    @Data
    public static class EntryPointRules {
        private Boolean enabled = true;
        private List<String> rules;
        private Map<String, String> customFunctions = Map.of();
    }
    
    /**
     * 依赖注入规则配置（预留扩展）
     */
    @Data
    public static class DependencyInjectionRules {
        private Boolean enabled = false;
        private DependencyInjectionFactMapping factMapping = new DependencyInjectionFactMapping();
        private DependencyInjectionResolutionRules resolutionRules = new DependencyInjectionResolutionRules();
        private Map<String, String> customFunctions = Map.of();
        private List<DependencyInjectionRule> rules;
    }

    @Data
    public static class DependencyInjectionFactMapping {
        private List<ProviderAnnotationMapping> providerAnnotations = List.of();
        private List<InjectionAnnotationMapping> injectionAnnotations = List.of();
        private List<QualifierAnnotationMapping> qualifierAnnotations = List.of();
        private List<PriorityAnnotationMapping> priorityAnnotations = List.of();
    }

    @Data
    public static class DependencyInjectionResolutionRules {
        private List<String> order = List.of(
                "qualifier",
                "requestedName",
                "memberName",
                "requiredTypeDefaultName",
                "priority",
                "uniqueCandidate");
    }

    @Data
    public static class ProviderAnnotationMapping {
        private String annotation;
        private String target = "TYPE";
        private List<String> nameAttributes = List.of("value");
        private String fallbackNameSource = "typeSimpleNameLowerCamel";
    }

    @Data
    public static class InjectionAnnotationMapping {
        private String annotation;
        private String mode;
        private String nameAttribute;
    }

    @Data
    public static class QualifierAnnotationMapping {
        private String annotation;
        private String attribute = "value";
    }

    @Data
    public static class PriorityAnnotationMapping {
        private String annotation;
        private Integer priority = 0;
    }

    @Data
    public static class DependencyInjectionRule implements MutationRuleDefinition {
        private String name;
        private String match;
        private String when;
        private String edgeType;
        private EdgeCategory edgeCategory = EdgeCategory.FRAMEWORK;
        private Integer priority = 0;
        private Map<String, Object> emitProperties = Map.of();
    }
    
    /**
     * RPC规则配置（预留扩展）
     */
    @Data
    public static class RpcRules {
        private Boolean enabled = false;
        private List<String> rules;
    }
}

