package com.puti.code.base.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphStorageTypeTest {

    @Test
    void shouldFallbackToNebulaWhenValueIsBlankOrUnknown() {
        assertEquals(GraphStorageType.NEBULA, GraphStorageType.fromValue(null));
        assertEquals(GraphStorageType.NEBULA, GraphStorageType.fromValue(""));
        assertEquals(GraphStorageType.NEBULA, GraphStorageType.fromValue("unknown"));
    }

    @Test
    void shouldResolveConfiguredStorageTypeIgnoringCase() {
        assertEquals(GraphStorageType.LOCAL_FILE, GraphStorageType.fromValue("local-file"));
        assertEquals(GraphStorageType.LOCAL_FILE, GraphStorageType.fromValue("local_file"));
        assertEquals(GraphStorageType.NEO4J, GraphStorageType.fromValue("NEO4J"));
    }
}
