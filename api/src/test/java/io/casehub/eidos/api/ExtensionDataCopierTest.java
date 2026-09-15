package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionDataCopierTest {

    @Test
    void deepCopy_nestedMap_producesSeparateCopy() {
        var inner = new LinkedHashMap<String, Object>();
        inner.put("key", "value");
        var original = new LinkedHashMap<String, Object>();
        original.put("nested", inner);

        var copy = ExtensionDataCopier.deepCopy(original);

        assertEquals(original, copy);
        inner.put("mutated", "yes");
        assertNull(((Map<?, ?>) copy.get("nested")).get("mutated"));
    }

    @Test
    void deepCopy_nestedList_producesSeparateCopy() {
        var list = new ArrayList<>(List.of("a", "b"));
        var original = new LinkedHashMap<String, Object>();
        original.put("items", list);

        var copy = ExtensionDataCopier.deepCopy(original);

        assertEquals(original, copy);
        list.add("c");
        assertEquals(2, ((List<?>) copy.get("items")).size());
    }

    @Test
    void deepCopy_primitivesPassedThrough() {
        var original = new LinkedHashMap<String, Object>();
        original.put("str", "hello");
        original.put("num", 42);
        original.put("dbl", 3.14);
        original.put("bool", true);
        original.put("nil", null);

        var copy = ExtensionDataCopier.deepCopy(original);

        assertEquals("hello", copy.get("str"));
        assertEquals(42, copy.get("num"));
        assertEquals(3.14, copy.get("dbl"));
        assertEquals(true, copy.get("bool"));
        assertNull(copy.get("nil"));
        assertTrue(copy.containsKey("nil"));
    }

    @Test
    void deepCopy_unknownType_throws() {
        var original = new LinkedHashMap<String, Object>();
        original.put("bad", new Object());

        assertThrows(IllegalArgumentException.class,
            () -> ExtensionDataCopier.deepCopy(original));
    }

    @Test
    void deepCopy_emptyMap() {
        var copy = ExtensionDataCopier.deepCopy(Map.of());
        assertTrue(copy.isEmpty());
    }

    @Test
    void deepCopy_deeplyNested() {
        var level3 = new LinkedHashMap<String, Object>();
        level3.put("deep", "value");
        var level2 = new LinkedHashMap<String, Object>();
        level2.put("inner", level3);
        var level1 = new LinkedHashMap<String, Object>();
        level1.put("outer", level2);

        var copy = ExtensionDataCopier.deepCopy(level1);

        level3.put("mutated", "yes");
        var copiedLevel2 = (Map<?, ?>) copy.get("outer");
        var copiedLevel3 = (Map<?, ?>) copiedLevel2.get("inner");
        assertNull(copiedLevel3.get("mutated"));
        assertEquals("value", copiedLevel3.get("deep"));
    }

    @Test
    void deepCopy_listWithMaps() {
        var mapInList = new LinkedHashMap<String, Object>();
        mapInList.put("k", "v");
        var list = new ArrayList<Object>();
        list.add(mapInList);
        var original = new LinkedHashMap<String, Object>();
        original.put("items", list);

        var copy = ExtensionDataCopier.deepCopy(original);

        mapInList.put("mutated", "yes");
        var copiedList = (List<?>) copy.get("items");
        var copiedMap = (Map<?, ?>) copiedList.get(0);
        assertNull(copiedMap.get("mutated"));
    }

    @Test
    void estimateSize_emptyMap_returns2() {
        assertEquals(2, ExtensionDataCopier.estimateSize(Map.of()));
    }

    @Test
    void estimateSize_simpleEntry() {
        var map = new LinkedHashMap<String, Object>();
        map.put("key", "value");
        long size = ExtensionDataCopier.estimateSize(map);
        assertTrue(size > 0);
        assertTrue(size < 100);
    }

    @Test
    void estimateSize_largeMap_exceedsLimit() {
        var map = new LinkedHashMap<String, Object>();
        map.put("huge", "x".repeat(60000));
        assertTrue(ExtensionDataCopier.estimateSize(map) > 60000);
    }

    @Test
    void estimateSize_nestedStructure() {
        var inner = new LinkedHashMap<String, Object>();
        inner.put("a", "b");
        var map = new LinkedHashMap<String, Object>();
        map.put("nested", inner);
        long size = ExtensionDataCopier.estimateSize(map);
        assertTrue(size > 0);
    }
}
