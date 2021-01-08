package dev.latvian.mods.rhino.util.wrap;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class TypeWrappers
{
	private final Map<TypeWrapperKey, TypeWrapper<?>> wrapperMap = new HashMap<>();

	public <F, T> void register(String id, Class<F> from, Class<T> to, Function<F, T> factory)
	{
		TypeWrapper<T> wrapper = new TypeWrapper<>(to, (Function<Object, Object>) factory);
		wrapperMap.put(new TypeWrapperKey("", from, to), wrapper);
		wrapperMap.put(new TypeWrapperKey(id, from, to), wrapper);
	}

	@Nullable
	public TypeWrapper<?> getWrapper(String id, Object from, Class<?> to)
	{
		return id == null || from == null ? null : wrapperMap.get(new TypeWrapperKey(id, from.getClass(), to));
	}
}
