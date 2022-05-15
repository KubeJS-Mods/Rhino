package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.function.IfFuncUnit;

public record TernaryOperatorUnitToken(InterpretableUnitToken cond, InterpretableUnitToken ifTrue, InterpretableUnitToken ifFalse) implements InterpretableUnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		var func = new IfFuncUnit();
		func.args[0] = cond.interpret(context);
		func.args[1] = ifTrue.interpret(context);
		func.args[2] = ifFalse.interpret(context);
		return func;
	}

	@Override
	public String toString() {
		return "Ternary[" + cond + " ? " + ifTrue + " : " + ifFalse + ']';
	}
}
