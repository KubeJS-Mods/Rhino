package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class Log1pFuncUnit extends FuncUnit {
	public Log1pFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.log1p(args[0].get(variables));
	}
}
