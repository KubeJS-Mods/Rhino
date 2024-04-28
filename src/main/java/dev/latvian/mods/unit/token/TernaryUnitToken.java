package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.TernaryUnit;
import dev.latvian.mods.unit.Unit;

public record TernaryUnitToken(UnitToken cond, UnitToken ifTrue, UnitToken ifFalse) implements UnitToken {
	@Override
	public Unit interpret(UnitTokenStream stream) {
		return new TernaryUnit(cond.interpret(stream), ifTrue.interpret(stream), ifFalse.interpret(stream));
	}

	@Override
	public String toString() {
		return "(" + cond + " ? " + ifTrue + " : " + ifFalse + ")";
	}
}
