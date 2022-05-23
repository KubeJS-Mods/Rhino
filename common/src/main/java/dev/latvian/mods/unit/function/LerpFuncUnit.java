package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class LerpFuncUnit extends Func3Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of3("lerp", Unit::lerp);

	public static double lerp(double a, double b, double c) {
		return b + a * (c - b);
	}

	public LerpFuncUnit(Unit a, Unit b, Unit c) {
		super(FACTORY, a, b, c);
	}

	@Override
	public double get(UnitVariables variables) {
		return lerp(a.get(variables), b.get(variables), c.get(variables));
	}
}
