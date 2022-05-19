package dev.latvian.mods.unit;

public abstract class Unit {
	public static Unit of(double value) {
		return FixedNumberUnit.ofFixed(value);
	}

	public abstract double get(UnitVariables variables);

	public float getFloat(UnitVariables variables) {
		return (float) get(variables);
	}

	public int getInt(UnitVariables variables) {
		double d = get(variables);
		int i = (int) d;
		return d < (double) i ? i - 1 : i;
	}

	public boolean getBoolean(UnitVariables variables) {
		return get(variables) != 0D;
	}

	public Unit optimize() {
		return this;
	}

	public void toString(StringBuilder builder) {
		builder.append(this);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		toString(builder);
		return builder.toString();
	}
}
