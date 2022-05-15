package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.function.FuncUnit;

import java.util.List;

public record FunctionUnitToken(String name, List<InterpretableUnitToken> args) implements InterpretableUnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		FuncUnit func = context.getFunction(name);

		if (func == null) {
			throw new IllegalStateException(String.format("Unknown function: %s", name));
		} else if (args.size() != func.args.length) {
			throw new IllegalStateException(String.format("Function %s requires %d arguments but found %d!", name, func.args.length, args.size()));
		}

		for (int i = 0; i < func.args.length; i++) {
			func.args[i] = args.get(i).interpret(context);
		}

		return func;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(name);
		builder.append('(');

		for (int i = 0; i < args.size(); i++) {
			if (i > 0) {
				builder.append(',');
			}

			builder.append(args.get(i).toString());
		}

		builder.append(')');
		return builder.toString();
	}
}
