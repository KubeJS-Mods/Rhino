package dev.latvian.mods.rhino.util.wrap;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public final class TypeWrapperKey
{
	public final String id;
	public final Class<?> from;
	public final Class<?> to;

	TypeWrapperKey(String i, Class<?> f, Class<?> t)
	{
		id = i;
		from = f;
		to = t;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}

		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		TypeWrapperKey that = (TypeWrapperKey) o;
		return Objects.equals(id, that.id) && Objects.equals(from, that.from) && Objects.equals(to, that.to);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, from, to);
	}

	@Override
	public String toString()
	{
		return "TypeWrapperKey{" +
				"id='" + id + '\'' +
				", from=" + from +
				", to=" + to +
				'}';
	}
}
