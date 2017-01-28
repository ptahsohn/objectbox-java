package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import io.objectbox.query.QueryBuilder.StringOrder;


import static io.objectbox.TestEntity_.simpleFloat;
import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleLong;
import static io.objectbox.TestEntity_.simpleShort;
import static io.objectbox.TestEntity_.simpleString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class QueryTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testBuild() {
        Query query = box.query().build();
        assertNotNull(query);
    }

    @Test
    public void testNullNotNull() {
        List<TestEntity> scalars = putTestEntitiesScalars();
        List<TestEntity> strings = putTestEntitiesStrings();
        assertEquals(strings.size(), box.query().notNull(simpleString).build().count());
        assertEquals(scalars.size(), box.query().isNull(simpleString).build().count());
    }

    @Test
    public void testScalarEqual() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query().equal(simpleInt, 2007).build();
        assertEquals(1, query.count());
        assertEquals(8, query.findFirst().getId());
        assertEquals(8, query.findUnique().getId());
        List<TestEntity> all = query.find();
        assertEquals(1, all.size());
        assertEquals(8, all.get(0).getId());
    }

    @Test
    public void testNoConditions() {
        List<TestEntity> entities = putTestEntitiesScalars();
        Query<TestEntity> query = box.query().build();
        List<TestEntity> all = query.find();
        assertEquals(entities.size(), all.size());
        assertEquals(entities.size(), query.count());
    }

    @Test
    public void testScalarNotEqual() {
        List<TestEntity> entities = putTestEntitiesScalars();
        Query<TestEntity> query = box.query().notEqual(simpleInt, 2007).notEqual(simpleInt, 2002).build();
        assertEquals(entities.size() - 2, query.count());
    }

    @Test
    public void testScalarLessAndGreater() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleInt, 2003).less(simpleShort, 2107).build();
        assertEquals(3, query.count());
    }

    @Test
    public void testScalarBetween() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(simpleInt, 2003, 2006).build();
        assertEquals(4, query.count());
    }

    @Test
    public void testScalarIn() {
        putTestEntitiesScalars();

        int[] valuesInt = {1, 1, 2, 3, 2003, 2007, 2002, -1};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        assertEquals(3, query.count());

        long[] valuesLong = {1, 1, 2, 3, 3003, 3007, 3002, -1};
        query = box.query().in(simpleLong, valuesLong).build();
        assertEquals(3, query.count());
    }

    @Test
    public void testOffsetLimit() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleInt, 2002).less(simpleShort, 2108).build();
        assertEquals(5, query.count());
        assertEquals(4, query.find(1, 0).size());
        assertEquals(1, query.find(4, 0).size());
        assertEquals(2, query.find(0, 2).size());
        List<TestEntity> list = query.find(1, 2);
        assertEquals(2, list.size());
        assertEquals(2004, list.get(0).getSimpleInt());
        assertEquals(2005, list.get(1).getSimpleInt());
    }

    @Test
    public void testAggregates() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().less(simpleInt, 2002).build();
        assertEquals(2000.5, query.avg(simpleInt), 0.0001);
        assertEquals(2000, query.min(simpleInt), 0.0001);
        assertEquals(400, query.minDouble(simpleFloat), 0.001);
        assertEquals(2001, query.max(simpleInt), 0.0001);
        assertEquals(400.1, query.maxDouble(simpleFloat), 0.001);
        assertEquals(4001, query.sum(simpleInt), 0.0001);
        assertEquals(800.1, query.sumDouble(simpleFloat), 0.001);
    }

    @Test
    public void testString() {
        List<TestEntity> entities = putTestEntitiesStrings();
        int count = entities.size();
        assertEquals(1, box.query().equal(simpleString, "banana").build().findUnique().getId());
        assertEquals(count - 1, box.query().notEqual(simpleString, "banana").build().count());
        assertEquals(4, box.query().startsWith(simpleString, "ba").endsWith(simpleString, "shake").build().findUnique()
                .getId());
        assertEquals(2, box.query().contains(simpleString, "nana").build().count());
    }

    @Test
    public void testScalarFloatLessAndGreater() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleFloat, 400.29f).less(simpleFloat, 400.51f).build();
        assertEquals(3, query.count());
    }

    @Test
    // Android JNI seems to have a limit of 512 local jobject references. Internally, we must delete those temporary
    // references when processing lists. This is the test for that.
    public void testBigResultList() {
        List<TestEntity> entities = new ArrayList<>();
        String sameValueForAll = "schrodinger";
        for (int i = 0; i < 10000; i++) {
            TestEntity entity = createTestEntity(sameValueForAll, i);
            entities.add(entity);
        }
        box.put(entities);
        int count = entities.size();
        List<TestEntity> entitiesQueried = box.query().equal(simpleString, sameValueForAll).build().find();
        assertEquals(count, entitiesQueried.size());
    }

    @Test
    public void testEqualStringOrder() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        assertEquals(2, box.query().equal(simpleString, "bar").build().count());
        assertEquals(1, box.query().equal(simpleString, "bar", StringOrder.CASE_SENSITIVE).build().count());
    }

    @Test
    public void testOrder() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        List<TestEntity> result = box.query().order(simpleString).build().find();
        assertEquals(6, result.size());
        assertEquals("apple", result.get(0).getSimpleString());
        assertEquals("banana", result.get(1).getSimpleString());
        assertEquals("banana milk shake", result.get(2).getSimpleString());
        assertEquals("bar", result.get(3).getSimpleString());
        assertEquals("BAR", result.get(4).getSimpleString());
        assertEquals("foo bar", result.get(5).getSimpleString());
    }

    @Test
    public void testOrderDescCaseNullLast() {
        putTestEntity(null, 1000);
        putTestEntity("BAR", 100);
        putTestEntitiesStrings();
        int flags = QueryBuilder.CASE_SENSITIVE | QueryBuilder.NULLS_LAST | QueryBuilder.DESCENDING;
        List<TestEntity> result = box.query().order(simpleString, flags).build().find();
        assertEquals(7, result.size());
        assertEquals("foo bar", result.get(0).getSimpleString());
        assertEquals("bar", result.get(1).getSimpleString());
        assertEquals("banana milk shake", result.get(2).getSimpleString());
        assertEquals("banana", result.get(3).getSimpleString());
        assertEquals("apple", result.get(4).getSimpleString());
        assertEquals("BAR", result.get(5).getSimpleString());
        assertNull(result.get(6).getSimpleString());
    }

    @Test
    public void testRemove() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleInt, 2003).build();
        assertEquals(6, query.remove());
        assertEquals(4, box.count());
    }

    @Test
    public void testFindKeysUnordered() {
        putTestEntitiesScalars();
        assertEquals(10, box.query().build().findIds().length);

        Query<TestEntity> query = box.query().greater(simpleInt, 2006).build();
        long[] keys = query.findIds();
        assertEquals(3, keys.length);
        assertEquals(8, keys[0]);
        assertEquals(9, keys[1]);
        assertEquals(10, keys[2]);
    }

    @Test
    public void testSetParameterInt() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().equal(simpleInt, 2007).build();
        assertEquals(8, query.findUnique().getId());
        query.setParameter(simpleInt, 2004);
        assertEquals(5, query.findUnique().getId());
    }

    @Test
    public void testSetParameter2Ints() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(simpleInt, 2005, 2008).build();
        assertEquals(4, query.count());
        query.setParameters(simpleInt, 2002, 2003);
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(3, entities.get(0).getId());
        assertEquals(4, entities.get(1).getId());
    }

    @Test
    public void testSetParameterFloat() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleFloat, 400.65).build();
        assertEquals(3, query.count());
        query.setParameter(simpleFloat, 400.75);
        assertEquals(2, query.count());
    }

    @Test
    public void testSetParameter2Floats() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(simpleFloat, 400.15, 400.75).build();
        assertEquals(6, query.count());
        query.setParameters(simpleFloat, 400.65, 400.85);
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(8, entities.get(0).getId());
        assertEquals(9, entities.get(1).getId());
    }

    @Test
    public void testSetParameterString() {
        putTestEntitiesStrings();
        Query<TestEntity> query = box.query().equal(simpleString, "banana").build();
        assertEquals(1, query.findUnique().getId());
        query.setParameter(simpleString, "bar");
        assertEquals(3, query.findUnique().getId());

        assertNull(query.setParameter(simpleString, "not here!").findUnique());
    }

    private List<TestEntity> putTestEntitiesScalars() {
        return putTestEntities(10, null, 2000);
    }

    private List<TestEntity> putTestEntitiesStrings() {
        List<TestEntity> entities = new ArrayList<>();
        entities.add(createTestEntity("banana", 1));
        entities.add(createTestEntity("apple", 2));
        entities.add(createTestEntity("bar", 3));
        entities.add(createTestEntity("banana milk shake", 4));
        entities.add(createTestEntity("foo bar", 5));
        box.put(entities);
        return entities;
    }

}
