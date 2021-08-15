package dev.latvian.mods.rhino.util.unit;

@FunctionalInterface
public interface OpSupplier {
	Unit create(Unit unit, Unit with);
}
