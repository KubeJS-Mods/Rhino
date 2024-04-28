package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class WithAlphaFuncUnit extends Func2Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of2("withAlpha", Unit::withAlpha);

	public WithAlphaFuncUnit(Unit a, Unit b) {
		super(FACTORY, a, b);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.atan2(a.get(variables), b.get(variables));
	}
}
