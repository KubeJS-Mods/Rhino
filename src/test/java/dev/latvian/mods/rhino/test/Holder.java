package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;

public record Holder<T>(T value) {
	public static Holder of(Context cx, Object from, TypeInfo target) {
		var type = target.param(0);

		if (type.convert()) {
			return new Holder(cx.jsToJava(from, type));
		}

		return new Holder(from);
	}
}
