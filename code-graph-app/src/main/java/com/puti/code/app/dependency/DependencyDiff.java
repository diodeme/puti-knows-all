package com.puti.code.app.dependency;

import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 依赖差异计算工具。
 * 对比前后两次依赖列表，计算新增、删除、版本变更和未变更的依赖。
 */
public class DependencyDiff {

    private DependencyDiff() {}

    /**
     * 计算两次依赖列表的差异。
     * GAV 格式为 groupId:artifactId:version，scope 变更不算版本变更（归入 unchanged）。
     *
     * @param previous 上次依赖列表（GAV → scope），可为 null 或空
     * @param current  当前依赖列表（GAV → scope），可为 null 或空
     * @return 差异结果
     */
    public static DiffResult diff(Map<String, String> previous, Map<String, String> current) {
        Map<String, String> prev = previous != null ? previous : Collections.emptyMap();
        Map<String, String> curr = current != null ? current : Collections.emptyMap();

        Set<String> prevKeys = prev.keySet();
        Set<String> currKeys = curr.keySet();

        // 新增：当前有但之前没有
        Set<String> added = new LinkedHashSet<>(currKeys);
        added.removeAll(prevKeys);

        // 删除：之前有但当前没有
        Set<String> removed = new LinkedHashSet<>(prevKeys);
        removed.removeAll(currKeys);

        // 交集：两边都有的 GAV
        Set<String> common = new HashSet<>(prevKeys);
        common.retainAll(currKeys);

        // 版本变更：GAV 相同意味着 groupId:artifactId:version 完全一致，
        // 所以 common 中的都是未变更的。真正的版本变更体现在 GAV 的 version 部分不同。
        // 例如之前 org.spring:spring-core:5.3.0 → 现在 org.spring:spring-core:5.3.1
        // 此时 GAV 不同，旧 GAV 出现在 removed，新 GAV 出现在 added。
        // 需要按 groupId:artifactId 匹配来检测版本变更。

        Set<String> changed = new LinkedHashSet<>();
        Set<String> unchanged = new LinkedHashSet<>(common);

        // 从 added 和 removed 中检测版本变更（同一 groupId:artifactId，不同 version）
        // oldMatched 记录被匹配到的旧 GAV，需从 removed 中移除
        Set<String> oldMatched = new LinkedHashSet<>();
        detectVersionChanges(added, removed, changed, oldMatched);

        // 版本变更的从 added 和 removed 中移除
        added.removeAll(changed);
        removed.removeAll(oldMatched);

        return new DiffResult(added, removed, changed, unchanged);
    }

    /**
     * 检测版本变更：added 和 removed 中存在相同 groupId:artifactId 但不同 version 的条目。
     * 匹配到的加入 changed 集合。
     */
    private static void detectVersionChanges(Set<String> added, Set<String> removed,
                                              Set<String> changed, Set<String> oldMatched) {
        for (String addedGav : added) {
            String gaKey = extractGroupArtifact(addedGav);
            if (gaKey == null) continue;

            for (String removedGav : removed) {
                String removedGaKey = extractGroupArtifact(removedGav);
                if (gaKey.equals(removedGaKey)) {
                    changed.add(addedGav);
                    oldMatched.add(removedGav);
                    break;
                }
            }
        }
    }

    /**
     * 从 GAV 中提取 groupId:artifactId 部分。
     * GAV 格式：groupId:artifactId:version
     */
    static String extractGroupArtifact(String gav) {
        if (gav == null) return null;
        int firstColon = gav.indexOf(':');
        int lastColon = gav.lastIndexOf(':');
        if (firstColon < 0 || lastColon <= firstColon) return null;
        return gav.substring(0, lastColon);
    }

    @Getter
    public static class DiffResult {
        /** 新增的依赖 GAV */
        private final Set<String> added;
        /** 删除的依赖 GAV */
        private final Set<String> removed;
        /** 版本变更的依赖（新的 GAV） */
        private final Set<String> changed;
        /** 未变更的依赖 GAV */
        private final Set<String> unchanged;

        DiffResult(Set<String> added, Set<String> removed, Set<String> changed, Set<String> unchanged) {
            this.added = Collections.unmodifiableSet(added);
            this.removed = Collections.unmodifiableSet(removed);
            this.changed = Collections.unmodifiableSet(changed);
            this.unchanged = Collections.unmodifiableSet(unchanged);
        }

        /** 是否有任何变化（新增、删除或版本变更） */
        public boolean hasChanges() {
            return !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
        }

        /** 需要处理的 GAV（新增 + 版本变更） */
        public Set<String> getAddedAndChanged() {
            Set<String> result = new LinkedHashSet<>(added);
            result.addAll(changed);
            return result;
        }
    }
}
