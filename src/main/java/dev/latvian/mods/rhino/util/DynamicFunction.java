package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class DynamicFunction extends BaseFunction
{
	@FunctionalInterface
	public interface Callback
	{
		@Nullable
		Object call(Object[] args);
	}

	private final Callback function;

	public DynamicFunction(Callback f)
	{
		function = f;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		return function.call(args);
	}
}