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
		if (stream.nextIf(SymbolUnitToken.LP)) {
			FuncUnit func = stream.context.getFunction(string);

			if (func == null) {
				throw new IllegalStateException("Unknown function: " + string);
			}

			int arg = 0;

			while (true) {
				// UnitToken token = next();
			}
		}

		return VariableUnit.of(string);
	}
}
