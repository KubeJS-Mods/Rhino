package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class DegFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("deg", Unit::deg);

	public DegFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.toDegrees(a.get(variables));
	}
}
