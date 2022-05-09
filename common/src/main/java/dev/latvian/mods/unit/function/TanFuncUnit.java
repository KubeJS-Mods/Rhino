package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class TanFuncUnit extends FuncUnit {
	public TanFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.tan(args[0].get(variables));
	}
}
