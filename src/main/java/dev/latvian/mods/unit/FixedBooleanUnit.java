package dev.latvian.mods.unit;

public class FixedBooleanUnit extends Unit {
	public static final FixedBooleanUnit TRUE = new FixedBooleanUnit(true);
	public static final FixedBooleanUnit FALSE = new FixedBooleanUnit(false);

	public final boolean value;

	private FixedBooleanUnit(boolean value) {
		this.value = value;
	}

	@Override
	public double get(UnitVariables variables) {
		return value ? 1D : 0D;
	}

	@Override
	public int getInt(UnitVariables variables) {
		return value ? 1 : 0;
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return value;
	}

	@Override
	public String toString() {
		return value ? "true" : "false";
	}

	@Override
	public Unit boolNot() {
		return value ? FALSE : TRUE;
	}

	@Override
	public Unit bool() {
		return this;
	}
}
