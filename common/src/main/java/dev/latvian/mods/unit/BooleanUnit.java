package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.InterpretableUnitToken;

public class BooleanUnit extends Unit implements InterpretableUnitToken {
	public static final BooleanUnit TRUE = new BooleanUnit(true);
	public static final BooleanUnit FALSE = new BooleanUnit(false);

	public static BooleanUnit of(boolean value) {
		return value ? TRUE : FALSE;
	}

	public final boolean value;

	private BooleanUnit(boolean value) {
		this.value = value;
	}

	@Override
	public double get(UnitVariables variables) {
		return value ? 1D : 0D;
	}

	@Override
	public float getFloat(UnitVariables variables) {
		return value ? 1F : 0F;
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
	public boolean equals(Object obj) {
		return obj == this || obj instanceof BooleanUnit u && value == u.value;
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(value);
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(value);
	}

	@Override
	public InterpretableUnitToken negate() {
		return of(!value);
	}

	@Override
	public InterpretableUnitToken bitNot() {
		return of(!value);
	}
}
