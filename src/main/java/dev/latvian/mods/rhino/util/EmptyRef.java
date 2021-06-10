package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Kit;
import dev.latvian.mods.rhino.Ref;

public class EmptyRef extends Ref {
	public static final EmptyRef INSTANCE = new EmptyRef();

	@Override
	public Object get(Context cx) {
		throw Kit.codeBug();
	}

	@Override
	public Object set(Context cx, Object value) {
		throw Kit.codeBug();
	}
}
