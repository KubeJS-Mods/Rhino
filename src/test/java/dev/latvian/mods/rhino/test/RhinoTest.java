package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.ContextFactory;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

public class RhinoTest {
	public final String testName;
	public final ContextFactory factory;
	public TestConsole console;
	public final Map<String, Object> shared;

	public RhinoTest(String n) {
		this.testName = n;
		this.factory = new TestContextFactory();
		this.console = new TestConsole(factory);
		this.shared = new HashMap<>();

		var typeWrappers = factory.getTypeWrappers();
		typeWrappers.registerDirect(TestMaterial.class, TestMaterial::get);
		typeWrappers.register(WithContext.class, WithContext::of);
	}

	public void test(String name, String script, String match) {
		try {
			var context = (TestContext) factory.enter();
			var rootScope = context.initStandardObjects();
			context.addToScope(rootScope, "console", console);
			context.addToScope(rootScope, "shared", shared);
			context.testName = name;
			context.evaluateString(rootScope, script, testName + "/" + name, 1, null);
		} catch (Exception ex) {
			ex.printStackTrace();
			console.info("Error: " + ex.getMessage());
			// ex.printStackTrace();
		}

		Assertions.assertEquals(match.trim(), console.getConsoleOutput().trim());
	}
}
