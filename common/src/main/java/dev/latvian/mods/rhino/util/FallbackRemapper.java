package dev.latvian.mods.rhino.util;

import java.lang.reflect.Member;

public record FallbackRemapper(Remapper main, Remapper fallback) implements Remapper {
	@Override
	public String remap(Class<?> from, Member member) {
		String s = main.remap(from, member);
		return s.isEmpty() ? fallback.remap(from, member) : s;
	}
}
