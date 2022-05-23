package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class AbsFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("abs", Unit::abs);

	public AbsFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.abs(a.get(variables));
	}
}
