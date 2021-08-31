package dev.latvian.mods.rhino.util.unit;

public class FixedUnit extends Unit {
	public static final FixedUnit ZERO = new FixedUnit(0F);
	public static final FixedUnit ONE = new FixedUnit(1F);
	public static final FixedUnit NAN = new FixedUnit(Float.NaN);
	public static final FixedUnit POS_INFINITY = new FixedUnit(Float.POSITIVE_INFINITY);
	public static final FixedUnit NEG_INFINITY = new FixedUnit(Float.NEGATIVE_INFINITY);

	public static FixedUnit of(float value) {
		if (Float.isNaN(value)) {
			return NAN;
		} else if (value == 0F) {
			return ZERO;
		} else if (value == 1F) {
			return ONE;
		} else if (value == Float.POSITIVE_INFINITY) {
			return POS_INFINITY;
		} else if (value == Float.NEGATIVE_INFINITY) {
			return NEG_INFINITY;
		}

		return new FixedUnit(value);
	}

	private final float value;

	protected FixedUnit(float v) {
		value = v;
	}

	@Override
	public float get() {
		return value;
	}

	@Override
	public boolean isFixed() {
		return true;
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append(get());
	}

	public Unit newValue(float v) {
		return get() == v ? this : of(v);
	}

	@Override
	public Unit neg() {
		return get() == 0F ? this : of(-get());
	}

	@Override
	public Unit add(Unit with) {
		return with.isFixed() ? newValue(get() + with.get()) : super.add(with);
	}

	@Override
	public Unit sub(Unit with) {
		return with.isFixed() ? newValue(get() - with.get()) : super.sub(with);
	}

	@Override
	public Unit mul(Unit with) {
		return with.isFixed() ? newValue(get() * with.get()) : super.mul(with);
	}

	@Override
	public Unit div(Unit with) {
		return with.isFixed() ? newValue(get() / with.get()) : super.div(with);
	}

	@Override
	public Unit mod(Unit with) {
		return with.isFixed() ? newValue(get() % with.get()) : super.mod(with);
	}

	@Override
	public Unit pow(Unit with) {
		return with.isFixed() ? newValue((float) Math.pow(get(), with.get())) : super.pow(with);
	}

	@Override
	public Unit min(Unit with) {
		return with.isFixed() ? newValue(Math.min(get(), with.get())) : super.min(with);
	}

	@Override
	public Unit max(Unit with) {
		return with.isFixed() ? newValue(Math.max(get(), with.get())) : super.max(with);
	}

	@Override
	public Unit shiftLeft(Unit with) {
		return with.isFixed() ? newValue((int) get() << (int) with.get()) : super.shiftLeft(with);
	}

	@Override
	public Unit shiftRight(Unit with) {
		return with.isFixed() ? newValue((int) get() >> (int) with.get()) : super.shiftRight(with);
	}

	@Override
	public Unit abs() {
		return newValue(Math.abs(get()));
	}

	@Override
	public Unit sin() {
		return newValue((float) Math.sin(get()));
	}

	@Override
	public Unit cos() {
		return newValue((float) Math.cos(get()));
	}

	@Override
	public Unit tan() {
		return newValue((float) Math.tan(get()));
	}

	@Override
	public Unit atan() {
		return newValue((float) Math.atan(get()));
	}

	@Override
	public Unit atan2(Unit with) {
		return with.isFixed() ? newValue((float) Math.atan2(get(), with.get())) : super.atan2(with);
	}

	@Override
	public Unit deg() {
		return newValue((float) Math.toDegrees(get()));
	}

	@Override
	public Unit rad() {
		return newValue((float) Math.toRadians(get()));
	}

	@Override
	public Unit log() {
		return newValue((float) Math.log(get()));
	}

	@Override
	public Unit log10() {
		return newValue((float) Math.log10(get()));
	}

	@Override
	public Unit log1p() {
		return newValue((float) Math.log1p(get()));
	}

	@Override
	public Unit sqrt() {
		return newValue((float) Math.sqrt(get()));
	}

	@Override
	public Unit sq() {
		return newValue(get() * get());
	}

	@Override
	public Unit floor() {
		return newValue(FloorUnit.floor(get()));
	}

	@Override
	public Unit ceil() {
		return newValue(CeilUnit.ceil(get()));
	}
}
