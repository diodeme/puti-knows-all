package com.puti.code.base.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphStorageTypeTest {

    @Test
    void shouldFallbackToNebulaWhenValueIsBlank() {
        assertEquals(GraphStorageType.NEBULA, GraphStorageType.fromValue(null));
        assertEquals(GraphStorageType.NEBULA, GraphStorageType.fromValue(""));
    }

    @Test
    void shouldResolveConfiguredStorageTypeIgnoringCase() {
        assertEquals(GraphStorageType.LOCAL_FILE, GraphStorageType.fromValue("local-file"));
        assertEquals(GraphStorageType.LOCAL_FILE, GraphStorageType.fromValue("local_file"));
        assertEquals(GraphStorageType.NEO4J, GraphStorageType.fromValue("NEO4J"));
    }

    @Test
    void shouldFailFastWhenStorageTypeIsUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> GraphStorageType.fromValue("unknown"));
    }
}
