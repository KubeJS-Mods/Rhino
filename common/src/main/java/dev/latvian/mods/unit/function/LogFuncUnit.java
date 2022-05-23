package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class LogFuncUnit extends Func1Unit {
	public static final FunctionFactory FACTORY = FunctionFactory.of1("log", Unit::log);

	public LogFuncUnit(Unit a) {
		super(FACTORY, a);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.log(a.get(variables));
	}
}
