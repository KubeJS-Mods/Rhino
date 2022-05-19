package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.function.FuncUnit;

import java.util.List;

public record FunctionUnitToken(String name, List<UnitToken> args) implements UnitToken {
	@Override
	public Unit interpret(UnitTokenStream stream) {
		FuncUnit func = stream.context.getFunction(name);

		if (func == null) {
			throw new IllegalStateException("Unknown function '" + name + "'!");
		} else if (func.args.length != args.size()) {
			throw new IllegalStateException("Function '" + name + "' expects " + func.args.length + " arguments, but " + args.size() + " were given!");
		}

		for (int i = 0; i < args.size(); i++) {
			func.args[i] = args.get(i).interpret(stream);
		}

		return func;
	}
}
