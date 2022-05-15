package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.VariableUnit;

public record NameUnitToken(String name) implements InterpretableUnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		return VariableUnit.of(name);
	}

	@Override
	public String toString() {
		return "Name[" + name + ']';
	}
}
