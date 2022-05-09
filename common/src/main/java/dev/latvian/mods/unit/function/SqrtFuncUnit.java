package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class SqrtFuncUnit extends FuncUnit {
	public SqrtFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.sqrt(args[0].get(variables));
	}
}
