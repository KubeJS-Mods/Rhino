package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * @author ZZZank
 */
public class TypeConsolidatorTest {
	public static final RhinoTest TEST = new RhinoTest("type_consolidator");

	public record Gener<T>(RhinoTest test) {
		public static final Gener<?> NONE = new Gener<>(TEST);

		/**
		 * simulate what bindings will usually do, (also there's no other easy way of passing full type info to JS)
		 */
		public static Gener<TestConsoleTheme> get() {
			return new Gener<>(TEST);
		}

		public void load(T input) {
			test.console.info("type: %s, value: %s".formatted(input.getClass(), input));
		}
	}

	static {
		TEST.shared.put("Gener", Gener.NONE);
	}

	@Test
	void test() {
		TEST.test("wrap_string", """
			shared.Gener.get().load("dArk")
			""", """
			type: %s, value: %s""".formatted(TestConsoleTheme.class, TestConsoleTheme.DARK));
	}
}
