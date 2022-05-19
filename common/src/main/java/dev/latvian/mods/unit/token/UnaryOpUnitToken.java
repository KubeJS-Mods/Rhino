package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.operator.UnaryOpUnit;

public record UnaryOpUnitToken(UnitSymbol operator, UnitToken token) implements UnitToken {
	@Override
	public Unit interpret(UnitTokenStream stream) {
		UnaryOpUnit unit = operator.unaryOp.create();
		unit.unit = token.interpret(stream);
		return unit;
	}

	@Override
	public String toString() {
		return "(" + operator + token + ")";
	}
}
