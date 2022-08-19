package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.SharedContextData;

@FunctionalInterface
public interface ValueUnwrapper {
	ValueUnwrapper DEFAULT = (contextData, scope, value) -> contextData.getWrapFactory().wrap(contextData, scope, value, value.getClass());

	Object unwrap(SharedContextData contextData, Scriptable scope, Object value);
}
