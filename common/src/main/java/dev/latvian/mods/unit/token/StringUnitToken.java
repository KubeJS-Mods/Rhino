package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.VariableUnit;
import dev.latvian.mods.unit.function.FuncUnit;

public class StringUnitToken implements UnitToken {
	public final String string;

	public StringUnitToken(String s) {
		string = s;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public Unit interpret(UnitTokenStream stream) {
		if (stream.nextTokenIf(SymbolUnitToken.LP)) {
			FuncUnit func = stream.context.getFunction(string);

			if (func == null) {
				throw new IllegalStateException("Unknown function: " + string);
			}

			for (int i = 0; i < func.args.length; i++) {
				func.args[i] = stream.nextUnit();
			}

			return func;
		}

		return VariableUnit.of(string);
	}
}
