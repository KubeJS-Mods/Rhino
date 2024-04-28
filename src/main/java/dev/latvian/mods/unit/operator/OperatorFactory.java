package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;

@FunctionalInterface
public interface OperatorFactory {
	Unit create(Unit left, Unit right);
}
