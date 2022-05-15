package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.operator.BitNotOpUnit;

public record BitNotUnitToken(InterpretableUnitToken token) implements InterpretableUnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		var unit = new BitNotOpUnit();
		unit.unit = token.interpret(context);
		return unit;
	}

	@Override
	public InterpretableUnitToken bitNot() {
		return token;
	}

	@Override
	public String toString() {
		return "BitNot[" + token.toString() + ']';
	}
}
