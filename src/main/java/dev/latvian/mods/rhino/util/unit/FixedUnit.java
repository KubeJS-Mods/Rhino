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
	public Unit neg() {
		return this == ZERO ? this : newValue(-get());
	}
}
