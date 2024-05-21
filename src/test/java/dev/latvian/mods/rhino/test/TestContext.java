package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.EvaluatorException;
import dev.latvian.mods.rhino.util.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TestContext extends Context {
	public String testName = "";

	public TestContext(TestContextFactory factory) {
		super(factory);
	}

	@Override
	public int internalConversionWeight(Object fromObj, Class<?> target, Type genericTarget) {
		if (target == WithContext.class) {
			return CONVERSION_NONTRIVIAL;
		}

		return super.internalConversionWeight(fromObj, target, genericTarget);
	}
}
