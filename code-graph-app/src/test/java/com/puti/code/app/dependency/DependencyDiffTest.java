package com.puti.code.app.dependency;

import com.puti.code.app.dependency.DependencyDiff.DiffResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyDiffTest {

    @Test
    void diffWithBothNullReturnsEmptyResult() {
        DiffResult result = DependencyDiff.diff(null, null);
        assertTrue(result.getAdded().isEmpty());
        assertTrue(result.getRemoved().isEmpty());
        assertTrue(result.getChanged().isEmpty());
        assertTrue(result.getUnchanged().isEmpty());
        assertFalse(result.hasChanges());
    }

    @Test
    void diffWithEmptyMapsReturnsEmptyResult() {
        DiffResult result = DependencyDiff.diff(Map.of(), Map.of());
        assertFalse(result.hasChanges());
    }

    @Test
    void detectAddedDependencies() {
        Map<String, String> previous = Map.of(
                "com.example:lib-a:1.0", "runtime"
        );
        Map<String, String> current = new LinkedHashMap<>();
        current.put("com.example:lib-a:1.0", "runtime");
        current.put("com.example:lib-b:2.0", "runtime");

        DiffResult result = DependencyDiff.diff(previous, current);

        assertTrue(result.getAdded().contains("com.example:lib-b:2.0"));
        assertEquals(1, result.getAdded().size());
        assertTrue(result.getUnchanged().contains("com.example:lib-a:1.0"));
        assertFalse(result.hasChanges() == result.getAdded().isEmpty());
    }

    @Test
    void detectRemovedDependencies() {
        Map<String, String> previous = new LinkedHashMap<>();
        previous.put("com.example:lib-a:1.0", "runtime");
        previous.put("com.example:lib-b:2.0", "runtime");
        Map<String, String> current = Map.of(
                "com.example:lib-a:1.0", "runtime"
        );

        DiffResult result = DependencyDiff.diff(previous, current);

        assertTrue(result.getRemoved().contains("com.example:lib-b:2.0"));
        assertEquals(1, result.getRemoved().size());
    }

    @Test
    void detectVersionChange() {
        Map<String, String> previous = Map.of(
                "org.springframework:spring-core:5.3.20", "runtime"
        );
        Map<String, String> current = Map.of(
                "org.springframework:spring-core:5.3.30", "runtime"
        );

        DiffResult result = DependencyDiff.diff(previous, current);

        // 新版本应出现在 changed，不在 added
        assertTrue(result.getChanged().contains("org.springframework:spring-core:5.3.30"));
        assertFalse(result.getAdded().contains("org.springframework:spring-core:5.3.30"));
        // 旧版本应从 removed 中移除（已归入 changed）
        assertFalse(result.getRemoved().contains("org.springframework:spring-core:5.3.20"));
        assertTrue(result.hasChanges());
    }

    @Test
    void detectUnchangedDependencies() {
        Map<String, String> deps = Map.of(
                "com.example:lib-a:1.0", "runtime",
                "com.example:lib-b:2.0", "runtime"
        );

        DiffResult result = DependencyDiff.diff(deps, deps);

        assertEquals(2, result.getUnchanged().size());
        assertTrue(result.getUnchanged().contains("com.example:lib-a:1.0"));
        assertTrue(result.getUnchanged().contains("com.example:lib-b:2.0"));
        assertFalse(result.hasChanges());
    }

    @Test
    void getAddedAndChangedCombinesBoth() {
        Map<String, String> previous = Map.of(
                "com.example:lib-a:1.0", "runtime"
        );
        Map<String, String> current = new LinkedHashMap<>();
        current.put("com.example:lib-a:2.0", "runtime");  // 版本变更
        current.put("com.example:lib-b:1.0", "runtime");  // 新增

        DiffResult result = DependencyDiff.diff(previous, current);

        assertEquals(2, result.getAddedAndChanged().size());
        assertTrue(result.getAddedAndChanged().contains("com.example:lib-a:2.0"));
        assertTrue(result.getAddedAndChanged().contains("com.example:lib-b:1.0"));
    }

    @Test
    void firstRunAllDependenciesAreAdded() {
        Map<String, String> current = Map.of(
                "org.springframework.boot:spring-boot:2.7.0", "runtime",
                "org.mybatis:mybatis:3.5.13", "runtime"
        );

        DiffResult result = DependencyDiff.diff(null, current);

        assertEquals(2, result.getAdded().size());
        assertTrue(result.getAdded().contains("org.springframework.boot:spring-boot:2.7.0"));
        assertTrue(result.getAdded().contains("org.mybatis:mybatis:3.5.13"));
    }

    @Test
    void extractGroupArtifactReturnsCorrectPart() {
        assertEquals("org.springframework:spring-core",
                DependencyDiff.extractGroupArtifact("org.springframework:spring-core:5.3.30"));
        assertEquals("com.example:lib",
                DependencyDiff.extractGroupArtifact("com.example:lib:1.0"));
    }

    @Test
    void extractGroupArtifactReturnsNullForInvalidInput() {
        assertEquals(null, DependencyDiff.extractGroupArtifact(null));
        assertEquals(null, DependencyDiff.extractGroupArtifact(""));
        assertEquals(null, DependencyDiff.extractGroupArtifact("only-one-part"));
        assertEquals(null, DependencyDiff.extractGroupArtifact("two:parts"));
    }

    @Test
    void mixedChangesScenario() {
        Map<String, String> previous = new LinkedHashMap<>();
        previous.put("com.a:lib-x:1.0", "runtime");     // 版本变更 → lib-x:2.0
        previous.put("com.a:lib-y:1.0", "runtime");     // 未变更
        previous.put("com.a:lib-z:1.0", "runtime");     // 被删除

        Map<String, String> current = new LinkedHashMap<>();
        current.put("com.a:lib-x:2.0", "runtime");     // 版本变更
        current.put("com.a:lib-y:1.0", "runtime");     // 未变更
        current.put("com.a:lib-w:1.0", "runtime");     // 新增

        DiffResult result = DependencyDiff.diff(previous, current);

        assertEquals(1, result.getChanged().size());
        assertTrue(result.getChanged().contains("com.a:lib-x:2.0"));

        assertEquals(1, result.getAdded().size());
        assertTrue(result.getAdded().contains("com.a:lib-w:1.0"));

        assertEquals(1, result.getRemoved().size());
        assertTrue(result.getRemoved().contains("com.a:lib-z:1.0"));

        assertEquals(1, result.getUnchanged().size());
        assertTrue(result.getUnchanged().contains("com.a:lib-y:1.0"));

        assertTrue(result.hasChanges());
    }
}
