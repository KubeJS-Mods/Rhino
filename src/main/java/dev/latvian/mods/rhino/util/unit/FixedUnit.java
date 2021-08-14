package dev.latvian.mods.rhino.util.unit;

public class FixedUnit implements Unit {
	private final float value;

	FixedUnit(float v) {
		value = v;
	}

	@Override
	public float get() {
		return value;
	}

	@Override
	public String toString() {
		return String.valueOf(get());
	}

	private Unit newValue(float v) {
		return get() == v ? this : Unit.fixed(v);
	}

	@Override
	public Unit neg() {
		return this == ZERO ? this : newValue(-get());
	}

	@Override
	public Unit add(Unit with) {
		return with instanceof FixedUnit ? newValue(get() + with.get()) : Unit.super.add(with);
	}

	@Override
	public Unit sub(Unit with) {
		return with instanceof FixedUnit ? newValue(get() - with.get()) : Unit.super.sub(with);
	}

	@Override
	public Unit mul(Unit with) {
		return with instanceof FixedUnit ? newValue(get() * with.get()) : Unit.super.mul(with);
	}

	@Override
	public Unit div(Unit with) {
		return with instanceof FixedUnit ? newValue(get() / with.get()) : Unit.super.div(with);
	}

	@Override
	public Unit mod(Unit with) {
		return with instanceof FixedUnit ? newValue(get() % with.get()) : Unit.super.mod(with);
	}

	@Override
	public Unit pow(Unit with) {
		return with instanceof FixedUnit ? newValue((float) Math.pow(get(), with.get())) : Unit.super.pow(with);
	}

	@Override
	public Unit min(Unit with) {
		return with instanceof FixedUnit ? newValue(Math.min(get(), with.get())) : Unit.super.min(with);
	}

	@Override
	public Unit max(Unit with) {
		return with instanceof FixedUnit ? newValue(Math.max(get(), with.get())) : Unit.super.max(with);
	}

	@Override
	public Unit shiftLeft(Unit with) {
		return with instanceof FixedUnit ? newValue((int) get() << (int) with.get()) : Unit.super.shiftLeft(with);
	}

	@Override
	public Unit shiftRight(Unit with) {
		return with instanceof FixedUnit ? newValue((int) get() >> (int) with.get()) : Unit.super.shiftRight(with);
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
		return with instanceof FixedUnit ? newValue((float) Math.atan2(get(), with.get())) : Unit.super.atan2(with);
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
