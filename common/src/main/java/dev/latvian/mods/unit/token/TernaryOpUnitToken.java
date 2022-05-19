package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.operator.TernaryOpUnit;

public record TernaryOpUnitToken(UnitToken cond, UnitToken left, UnitToken right) implements UnitToken {
	@Override
	public Unit interpret(UnitTokenStream stream) {
		var unit = new TernaryOpUnit();
		unit.cond = cond.interpret(stream);
		unit.left = left.interpret(stream);
		unit.right = right.interpret(stream);
		return unit;
	}

	@Override
	public String toString() {
		return "(" + cond + " ? " + left + " : " + right + ")";
	}
}
