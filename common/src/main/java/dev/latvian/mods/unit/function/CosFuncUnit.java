package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class CosFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("cos", Unit::cos);

	public CosFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.cos(a.get(variables));
	}
}
