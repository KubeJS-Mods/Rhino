package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class SqFuncUnit extends FuncUnit {
	public SqFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		double x = args[0].get(variables);
		return x * x;
	}
}
