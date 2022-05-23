package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class Log1pFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("log1p", Unit::log1p);

	public Log1pFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.log1p(a.get(variables));
	}
}
