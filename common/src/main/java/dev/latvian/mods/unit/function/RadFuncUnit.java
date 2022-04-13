package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class RadFuncUnit extends FuncUnit {
	public RadFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.toRadians(args[0].get(variables));
	}
}
