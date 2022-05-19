package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.operator.OpUnit;

public record OpResultUnitToken(UnitSymbol operator, UnitToken left, UnitToken right) implements UnitToken {
	@Override
	public Unit interpret(UnitTokenStream stream) {
		OpUnit unit = operator.op.create();
		unit.left = left.interpret(stream);
		unit.right = right.interpret(stream);
		return unit;
	}

	@Override
	public String toString() {
		return "(" + left + " " + operator + " " + right + ")";
	}
}
