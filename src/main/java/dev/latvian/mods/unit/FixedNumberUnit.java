package dev.latvian.mods.unit;

public final class FixedNumberUnit extends Unit {
	public static final FixedNumberUnit ZERO = new FixedNumberUnit(0);
	public static final FixedNumberUnit ONE = new FixedNumberUnit(1);
	public static final FixedNumberUnit MINUS_ONE = new FixedNumberUnit(-1);
	public static final FixedNumberUnit TEN = new FixedNumberUnit(10);
	public static final FixedNumberUnit SIXTEEN = new FixedNumberUnit(16);
	public static final FixedNumberUnit PI = new FixedNumberUnit(Math.PI);
	public static final FixedNumberUnit TWO_PI = new FixedNumberUnit(Math.PI * 2D);
	public static final FixedNumberUnit HALF_PI = new FixedNumberUnit(Math.PI / 2D);
	public static final FixedNumberUnit E = new FixedNumberUnit(Math.E);
	public static final FixedNumberUnit NaN = new FixedNumberUnit(Double.NaN);

	public static FixedNumberUnit of(double value) {
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

	public final double value;

	private FixedNumberUnit(double value) {
		this.value = value;
	}

	@Override
	public boolean isFixed() {
		return true;
	}

	@Override
	public double get(UnitVariables variables) {
		return value;
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return this != ZERO;
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
		long r = Math.round(value);

		if (Math.abs(r - value) < 0.00001D) {
			builder.append(r);
		} else {
			builder.append(value);
		}
	}

	// Operators

	@Override
	public Unit negate() {
		return of(-value);
	}

	@Override
	public Unit add(Unit other) {
		return other instanceof FixedNumberUnit u ? of(value + u.value) : super.add(other);
	}

	@Override
	public Unit add(double value) {
		return of(this.value + value);
	}

	@Override
	public Unit sub(Unit other) {
		return other instanceof FixedNumberUnit u ? of(value - u.value) : super.sub(other);
	}

	@Override
	public Unit sub(double value) {
		return of(this.value - value);
	}

	@Override
	public Unit mul(Unit other) {
		return other instanceof FixedNumberUnit u ? of(value * u.value) : super.mul(other);
	}

	@Override
	public Unit mul(double value) {
		return of(this.value * value);
	}

	@Override
	public Unit div(Unit other) {
		return other instanceof FixedNumberUnit u ? of(value / u.value) : super.div(other);
	}

	@Override
	public Unit div(double value) {
		return of(this.value / value);
	}

	@Override
	public Unit mod(Unit other) {
		return other instanceof FixedNumberUnit u ? of(value % u.value) : super.mod(other);
	}

	@Override
	public Unit mod(double value) {
		return of(this.value % value);
	}

	@Override
	public Unit pow(Unit other) {
		return other instanceof FixedNumberUnit u ? of(Math.pow(value, u.value)) : super.add(other);
	}

	// Functions

	@Override
	public Unit abs() {
		return of(Math.abs(value));
	}

	@Override
	public Unit sin() {
		return of(Math.sin(value));
	}

	@Override
	public Unit cos() {
		return of(Math.cos(value));
	}

	@Override
	public Unit tan() {
		return of(Math.tan(value));
	}

	@Override
	public Unit deg() {
		return of(Math.toDegrees(value));
	}

	@Override
	public Unit rad() {
		return of(Math.toRadians(value));
	}

	@Override
	public Unit atan() {
		return of(Math.atan(value));
	}

	@Override
	public Unit log() {
		return of(Math.log(value));
	}

	@Override
	public Unit log10() {
		return of(Math.log10(value));
	}

	@Override
	public Unit log1p() {
		return of(Math.log1p(value));
	}

	@Override
	public Unit sqrt() {
		return of(Math.sqrt(value));
	}

	@Override
	public Unit sq() {
		return of(value * value);
	}

	@Override
	public Unit floor() {
		return of(Math.floor(value));
	}

	@Override
	public Unit ceil() {
		return of(Math.ceil(value));
	}

	@Override
	public Unit bool() {
		return this == ZERO ? FixedBooleanUnit.FALSE : FixedBooleanUnit.TRUE;
	}

	@Override
	public Unit smoothstep() {
		return of(value * value * value * (value * (value * 6D - 15D) + 10D));
	}
}
