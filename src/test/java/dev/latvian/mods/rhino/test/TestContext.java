package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;

public class TestContext extends Context {
	public String testName = "";

	public TestContext(TestContextFactory factory) {
		super(factory);
	}

	@Override
	public String toString() {
		return testName;
	}
}
