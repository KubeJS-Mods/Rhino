package dev.latvian.mods.rhino.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

public class DefaultRemapper implements Remapper {
	public static final DefaultRemapper INSTANCE = new DefaultRemapper();

	private DefaultRemapper() {
	}

	@Override
	public String remap(Class<?> from, Member member) {
		if (member instanceof AnnotatedElement annotatedElement) {
			RemapForJS remap = annotatedElement.getAnnotation(RemapForJS.class);

			if (remap != null) {
				return remap.value();
			}
		}

		return "";
	}
}
