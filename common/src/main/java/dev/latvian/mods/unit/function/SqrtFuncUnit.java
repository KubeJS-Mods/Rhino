package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class SqrtFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("sqrt", Unit::sqrt);

	public SqrtFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.sqrt(a.get(variables));
	}
}
