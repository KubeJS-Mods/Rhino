package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.operator.NegateOpUnit;

public record NegateUnitToken(InterpretableUnitToken token) implements InterpretableUnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		var unit = new NegateOpUnit();
		unit.unit = token.interpret(context);
		return unit;
	}

	@Override
	public InterpretableUnitToken negate() {
		return token;
	}

	@Override
	public String toString() {
		return "Negate[" + token + ']';
	}
}
