package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class IfFuncUnit extends FuncUnit {
	public IfFuncUnit() {
		super(3);
	}

	@Override
	public double get(UnitVariables variables) {
		return getBoolean(variables) ? args[1].get(variables) : args[2].get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return getBoolean(variables) ? args[1].getInt(variables) : args[2].getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return args[0].getBoolean(variables);
	}
}
