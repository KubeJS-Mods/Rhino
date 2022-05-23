package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class SinFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("sin", Unit::sin);

	public SinFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.sin(a.get(variables));
	}
}
