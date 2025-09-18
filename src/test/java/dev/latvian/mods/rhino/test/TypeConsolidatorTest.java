package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZZZank
 */
public class TypeConsolidatorTest {
	public static final RhinoTest TEST = new RhinoTest("type_consolidator");

	public static final class Gener<T> {
		public static final Gener<?> WILDCARD = new Gener<>(TEST, new ArrayList<>());
		public static final Gener RAW = new Gener<>(TEST, new ArrayList<>());
		public static final Gener<TestConsoleTheme> ENUM = new Gener<>(TEST, new ArrayList<>());

		private final RhinoTest test;
		private final List<T> loaded;
		/**
		 * test type consolidation for field
		 */
		public T example = null;

		public Gener(RhinoTest test, List<T> loaded) {
			this.test = test;
			this.loaded = loaded;
		}

		/**
		 * test type consolidation for param
		 */
		public void load(T input) {
			loaded.add(input);
			test.console.info("type: %s, value: %s".formatted(input.getClass(), input));
		}

		/**
		 * test type consolidation for return
		 */
		public T get(int index) {
			return loaded.get(index);
		}

		@Override
		public String toString() {
			return "Gener[test=%s, loaded=%s]".formatted(test, loaded);
		}
	}

	static {
		TEST.scopeAction = (cx, scope) -> {
			cx.addToScope(scope, "Gener", Gener.class);
		};
	}

	@Test
	void testGeneric() {
		TEST.test("get_set", """
			const g = Gener.ENUM;
			
			// method
			g.load("dArk");
			console.info(g.get(0));

			// field
			g.example = "light"
			console.info(g.example);
			""", """
			type: %s, value: %s
			%s
			%s""".formatted(
			TestConsoleTheme.class, TestConsoleTheme.DARK,
			TestConsoleTheme.DARK,
			TestConsoleTheme.LIGHT));
	}

	@Test
	void testRaw() {
		TEST.test("get_set", """
			const g = Gener.RAW;

			g.load("dArk");
			console.info(g.get(0));

			g.example = "light"
			console.info(g.example);
			""", """
			type: %s, value: %s
			%s
			%s""".formatted(String.class, "dArk", "dArk", "light"));
	}

	@Test
	void testWildcard() {
		TEST.test("get_set", """
			const g = Gener.WILDCARD;

			g.load("dArk");
			console.info(g.get(0));

			g.example = "light"
			console.info(g.example);
			""", """
			type: %s, value: %s
			%s
			%s""".formatted(String.class, "dArk", "dArk", "light"));
	}
}
