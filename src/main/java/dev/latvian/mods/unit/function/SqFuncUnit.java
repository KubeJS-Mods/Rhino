package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class SqFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("sq", Unit::sq);

	public SqFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		double x = a.get(variables);
		return x * x;
	}
}
