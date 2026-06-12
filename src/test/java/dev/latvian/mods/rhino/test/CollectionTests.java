package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Tests for Map and Set.
 */
@SuppressWarnings("unused")
public class CollectionTests {
	public static class TestUtil {
		public static UUID one() {
			return new UUID(0L, 1L);
		}
	}

	public static final RhinoTest TEST = new RhinoTest("collections").withScopeAction((cx, rootScope) -> {
		cx.addToScope(rootScope, "TestUtil", TestUtil.class);
	});

	@Test
	public void jsCollectionsJavaObjectId() {
		TEST.test("jsCollectionsJavaObjectId", """
			const s = new Set();
			s.add(TestUtil.one());
			s.add(TestUtil.one());
			console.info(s.size);
			console.info(s.has(TestUtil.one()));
			const m = new Map();
			m.set(TestUtil.one(), 'first');
			m.set(TestUtil.one(), 'second');
			console.info(m.size);
			console.info(m.get(TestUtil.one()));
			""", """
			1
			true
			1
			second
			""");
	}

	@Test
	public void mapGroupBy() {
		TEST.test("mapGroupBy", """
			const m = Map.groupBy([1, 2, 3, 4, 5], x => x % 2 === 0 ? 'even' : 'odd');
			console.info(m instanceof Map);
			console.info(m.size);
			console.info(m.get('odd').join(','));
			console.info(m.get('even').join(','));
			const z = Map.groupBy([7], () => -0);
			console.info(1 / z.keys().next().value);
			""", """
			true
			2
			1,3,5
			2,4
			Infinity
			""");
	}
}
