package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class MaxFuncUnit extends Func2Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of2("max", Unit::max);

	public MaxFuncUnit(Unit a, Unit b) {
		super(FACTORY, a, b);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.max(a.get(variables), b.get(variables));
	}
}
