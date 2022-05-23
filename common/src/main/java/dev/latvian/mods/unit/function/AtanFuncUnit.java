package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class AtanFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("atan", Unit::atan);

	public AtanFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.atan(a.get(variables));
	}
}
