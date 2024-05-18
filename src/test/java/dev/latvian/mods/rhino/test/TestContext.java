package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.EvaluatorException;
import dev.latvian.mods.rhino.util.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TestContext extends Context {
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

	@Override
	protected Object internalJsToJava(Object value, Class<?> target, Type genericTarget) throws EvaluatorException {
		if (genericTarget instanceof ParameterizedType parameterizedType) {
			if (target == WithContext.class) {
				var types = parameterizedType.getActualTypeArguments();

				if (types.length == 1) {
					return new WithContext<>(this, jsToJava(value, TypeUtils.getRawType(types[0]), types[0]));
				}

				return new WithContext<>(this, value);
			}
		}

		return super.internalJsToJava(value, target, genericTarget);
	}
}
