package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Type;
import java.util.Map;

public class GenericObject<T> {
	public static final Map<String, String> ASSERTIONS = Map.of(
		"none", "?",
		"any", "?",
		"object", "?",
		"string", "java.lang.String",
		"anyString", "java.lang.CharSequence",
		"anySuperString", "java.lang.CharSequence",
		"t", "?",
		"tString", "java.lang.CharSequence",
		"k", "dev.latvian.mods.rhino.test.GenericObject<java.lang.CharSequence>"
	);

	public static String test = "";

	public static void test(String name, Type type) {
		var typeInfo = TypeInfo.of(type).param(0);
		Assertions.assertEquals(name + ": " + typeInfo, name + ": " + GenericObject.ASSERTIONS.get(name));
	}

	public GenericObject none() {
		return null;
	}

	public GenericObject<?> any() {
		return null;
	}

	public GenericObject<Object> object() {
		return null;
	}

	public GenericObject<String> string() {
		return null;
	}

	public GenericObject<? extends CharSequence> anyString() {
		return null;
	}

	public GenericObject<? super CharSequence> anySuperString() {
		return null;
	}

	public <T> GenericObject<T> t() {
		return null;
	}

	public GenericObject<? extends CharSequence> tString() {
		return null;
	}

	public GenericObject<? extends GenericObject<? extends CharSequence>> k() {
		return null;
	}
}
