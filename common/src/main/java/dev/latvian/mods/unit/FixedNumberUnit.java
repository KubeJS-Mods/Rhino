package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.UnitToken;

import java.util.HashMap;
import java.util.Map;

public final class FixedNumberUnit extends Unit implements UnitToken {
	public static final FixedNumberUnit ZERO = new FixedNumberUnit(0);
	public static final FixedNumberUnit ONE = new FixedNumberUnit(1);
	public static final FixedNumberUnit MINUS_ONE = new FixedNumberUnit(-1);
	public static final FixedNumberUnit TEN = new FixedNumberUnit(10);
	public static final FixedNumberUnit SIXTEEN = new FixedNumberUnit(16);
	public static final FixedNumberUnit PI = new FixedNumberUnit(Math.PI);
	public static final FixedNumberUnit E = new FixedNumberUnit(Math.E);

	public static FixedNumberUnit ofFixed(double value) {
		if (value == 0D) {
			return ZERO;
		} else if (value == 1D) {
			return ONE;
		} else if (value == -1D) {
			return MINUS_ONE;
		} else if (value == 10D) {
			return TEN;
		} else if (value == 16D) {
			return SIXTEEN;
		} else {
			return new FixedNumberUnit(value);
		}
	}

	public static final Map<String, FixedNumberUnit> CONSTANTS = new HashMap<>();
	public static final Map<FixedNumberUnit, String> CONSTANTS_INV = new HashMap<>();

	public static void addConstant(String s, FixedNumberUnit u) {
		CONSTANTS.put(s, u);
		CONSTANTS_INV.put(u, s);
	}

	static {
		addConstant("PI", PI);
		addConstant("E", E);
	}

	public final double value;

	private FixedNumberUnit(double value) {
		this.value = value;
	}

	@Override
	public double get(UnitVariables variables) {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof FixedNumberUnit u && value == u.value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public void toString(StringBuilder builder) {
		String s = CONSTANTS_INV.get(this);

		if (s != null) {
			builder.append(s);
		} else {
			builder.append(value);
		}
	}
}
