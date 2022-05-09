package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

@FunctionalInterface
public interface ValueUnwrapper {
	ValueUnwrapper DEFAULT = (scope, value) -> {
		Context cx = Context.getContext();
		return cx.getWrapFactory().wrap(cx, scope, value, value.getClass());
	};

	Object unwrap(Scriptable scope, Object value);
}
