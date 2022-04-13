package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class AtanFuncUnit extends FuncUnit {
	public AtanFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.atan(args[0].get(variables));
	}
}
