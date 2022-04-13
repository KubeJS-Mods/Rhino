package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class LogFuncUnit extends FuncUnit {
	public LogFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.log(args[0].get(variables));
	}
}
