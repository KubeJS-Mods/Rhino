package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class Atan2FuncUnit extends FuncUnit {
	public Atan2FuncUnit() {
		super(2);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.atan2(args[0].get(variables), args[1].get(variables));
	}
}
