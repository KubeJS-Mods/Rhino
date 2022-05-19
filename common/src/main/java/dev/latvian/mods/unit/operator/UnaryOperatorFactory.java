package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;

@FunctionalInterface
public interface UnaryOperatorFactory {
	Unit create(Unit unit);
}
