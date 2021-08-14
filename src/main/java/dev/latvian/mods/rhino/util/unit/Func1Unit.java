package dev.latvian.mods.rhino.util.unit;

public abstract class Func1Unit implements Unit {
	public final Unit unit;

	public Func1Unit(Unit u) {
		unit = u;
	}

	protected static String fString(String name, Object u) {
		return name + "(" + u + ")";
	}
}
