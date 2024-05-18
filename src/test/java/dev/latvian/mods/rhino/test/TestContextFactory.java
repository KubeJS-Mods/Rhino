package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;

public class TestContextFactory extends ContextFactory {
	@Override
	protected Context createContext() {
		return new TestContext(this);
	}
}
