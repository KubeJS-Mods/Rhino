package dev.latvian.mods.rhino.util.unit;

public abstract class Func2Unit implements Unit {
	public final Unit unit;
	public final Unit with;

	public Func2Unit(Unit u, Unit w) {
		unit = u;
		with = w;
	}

	protected static String aString(Object a, String join, Object b) {
		return "(" + a + join + b + ")";
	}

	protected static String fString(String name, Object a, Object b) {
		return name + "(" + a + ", " + b + ")";
	}
}
