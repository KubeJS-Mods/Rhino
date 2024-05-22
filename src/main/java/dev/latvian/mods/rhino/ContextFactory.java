package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.wrap.TypeWrappers;

public class ContextFactory {
	private final ThreadLocal<Context> currentContext;
	private final TypeWrappers typeWrappers;

	public ContextFactory() {
		this.currentContext = ThreadLocal.withInitial(this::createContext);
		this.typeWrappers = new TypeWrappers();
	}

	protected Context createContext() {
		return new Context(this);
	}

	// private Context unsafeContext;

	public Context enter() {
		// if (unsafeContext == null) {
		//	unsafeContext = createContext();
		// }

		// return unsafeContext;
		return currentContext.get();
	}

	public synchronized TypeWrappers getTypeWrappers() {
		return typeWrappers;
	}
}
