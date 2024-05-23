package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;

import java.util.Map;

public interface ToStringJS {
	static String toStringJS(Context cx, Object o) {
		o = Wrapper.unwrapped(o);

		return switch (o) {
			case null -> "null";
			case ToStringJS toStringJS -> toStringJS.toStringJS(cx);
			case Iterable<?> itr -> {
				var sb = new StringBuilder();
				sb.append('[');
				var first = true;

				for (var i : itr) {
					if (!first) {
						sb.append(", ");
					}

					sb.append(toStringJS(cx, i));
					first = false;
				}

				sb.append(']');
				yield sb.toString();
			}
			case Map<?, ?> map -> {
				var sb = new StringBuilder();
				boolean first = true;

				sb.append('{');

				for (var entry : map.entrySet()) {
					if (!first) {
						sb.append(", ");
					}

					sb.append(toStringJS(cx, entry.getKey()));
					sb.append(": ");
					sb.append(toStringJS(cx, entry.getValue()));
					first = false;
				}

				sb.append('}');
				yield sb.toString();
			}
			default -> o.toString();
		};
	}

	default String toStringJS(Context cx) {
		return toString();
	}
}
