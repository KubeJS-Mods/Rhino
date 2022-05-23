package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;

public abstract class FuncUnit extends Unit {
	private static final Unit[] NO_ARGS = new Unit[0];

	public final FunctionFactory factory;

	public FuncUnit(FunctionFactory factory) {
		this.factory = factory;
	}

	protected Unit[] getArguments() {
		return NO_ARGS;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(factory.name());
		builder.append('(');

		var args = getArguments();

		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				builder.append(',');
			}

			args[i].toString(builder);
		}

		builder.append(')');
	}
}
