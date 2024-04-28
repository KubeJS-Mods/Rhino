package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class SmoothstepFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("smoothstep", Unit::smoothstep);

	public SmoothstepFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		double d = a.get(variables);
		return d * d * d * (d * (d * 6D - 15D) + 10D);
	}
}
