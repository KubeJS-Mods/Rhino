package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class FloorFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("floor", Unit::floor);

	public FloorFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.floor(a.get(variables));
	}
}
