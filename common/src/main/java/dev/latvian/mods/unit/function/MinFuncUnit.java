package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class MinFuncUnit extends Func2Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of2("min", Unit::min);

	public MinFuncUnit(Unit a, Unit b) {
		super(FACTORY, a, b);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.min(a.get(variables), b.get(variables));
	}
}
