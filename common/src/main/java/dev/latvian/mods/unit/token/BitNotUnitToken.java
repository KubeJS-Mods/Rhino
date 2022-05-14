package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.operator.BitNotOpUnit;

public record BitNotUnitToken(UnitToken token) implements UnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		var unit = new BitNotOpUnit();
		unit.unit = token.interpret(context);
		return unit;
	}

	@Override
	public UnitToken bitNot() {
		return token;
	}

	@Override
	public String toString() {
		return "~" + token.toString();
	}
}
