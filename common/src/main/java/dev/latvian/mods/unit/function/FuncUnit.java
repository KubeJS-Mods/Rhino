package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;

public abstract class FuncUnit extends Unit {
	private static final Unit[] NO_ARGS = new Unit[0];

	public final Unit[] args;
	public FunctionFactory factory;

	public FuncUnit(int count) {
		args = count <= 0 ? NO_ARGS : new Unit[count];
	}

	@Override
	public FuncUnit optimize() {
		for (int i = 0; i < args.length; i++) {
			args[i] = args[i].optimize();
		}

		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(factory.name());
		builder.append('(');

		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				builder.append(',');
			}

			if (args[i] == null) {
				builder.append("null");
			} else {
				args[i].toString(builder);
			}
		}

		builder.append(')');
	}

	/*
	@Override
	public void interpret(UnitTokenStream tokenStream) {
		for (int i = args.length - 1; i >= 0; i--) {
			args[i] = tokenStream.resultStack.pop();
		}

		tokenStream.resultStack.push(this);
	}
	 */
}
