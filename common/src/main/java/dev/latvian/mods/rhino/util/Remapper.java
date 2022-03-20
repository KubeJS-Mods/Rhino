package dev.latvian.mods.rhino.util;

import java.lang.reflect.Member;

public interface Remapper {
	String remap(Class<?> from, Member member);
}
