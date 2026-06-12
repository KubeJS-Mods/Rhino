package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.JavaScriptException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Tests for features and fixes in the Java-JS interop layer.
 */
@SuppressWarnings("unused")
public class InteropTests {
	public static class TestUtil {
		public static UUID one() {
			return new UUID(0L, 1L);
		}

		public static void boom() {
			throw new IllegalStateException("kaBOOM");
		}
	}

	public static class InterfaceTests {
		public interface Defaulted {
			default String someMethod() {
				return "default";
			}
		}

		public interface DefaultedAbstract {
			String abstractMethod();

			default String someMethod() {
				return "default";
			}
		}

		public static String callDefaulted(Defaulted d) {
			return d.someMethod();
		}

		public static String callDefaultedAbstract(DefaultedAbstract d) {
			return d.someMethod() + "," + d.abstractMethod();
		}
	}

	public static final RhinoTest TEST = new RhinoTest("interopLayer").withScopeAction((cx, rootScope) -> {
		cx.addToScope(rootScope, "TestUtil", TestUtil.class);
		cx.addToScope(rootScope, "Interfaces", InterfaceTests.class);
	});



	@Test
	public void javaScriptExceptionPreservesCause() {
		var factory = new TestContextFactory();
		var cx = factory.enter();
		var scope = cx.initStandardObjects();
		cx.addToScope(scope, "TestUtil", TestUtil.class);
		try {
			cx.evaluateString(scope, "try { TestUtil.boom(); } catch (e) { throw e; }", "causeTest", 1, null);
			Assertions.fail("expected an exception");
		} catch (JavaScriptException ex) {
			Throwable cause = ex.getCause();
			while (cause != null && !(cause instanceof IllegalStateException)) {
				cause = cause.getCause();
			}
			Assertions.assertNotNull(cause, "IllegalStateException not in cause chain??");
			Assertions.assertEquals("kaBOOM", cause.getMessage());
		}
	}

	@Test
	public void overriddenDefaultMethod() {
		TEST.test("overriddenDefaultMethod", """
			console.info(Interfaces.callDefaulted({}))
			console.info(Interfaces.callDefaulted({ someMethod: () => 'overridden' }))
			console.info(Interfaces.callDefaultedAbstract({ someMethod: () => 'overridden', abstractMethod: () => 'js abstract' }))
			""", """
			default
			overridden
			overridden,js abstract
			""");
	}
}
