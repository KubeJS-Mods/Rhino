package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class TanFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("tan", Unit::tan);

	public TanFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.tan(a.get(variables));
	}
}
