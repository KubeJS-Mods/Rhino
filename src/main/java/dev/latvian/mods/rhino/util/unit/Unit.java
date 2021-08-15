package dev.latvian.mods.rhino.util.unit;

public abstract class Unit {
	public abstract float get();

	public int getAsInt() {
		return (int) get();
	}

	public boolean getAsBoolean() {
		return get() != 0F;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public boolean isFixed() {
		return false;
	}

	public abstract void append(StringBuilder sb);

	public Unit neg() {
		return new NegUnit(this);
	}

	public Unit add(Unit with) {
		return new AddUnit(this, with);
	}

	public Unit sub(Unit with) {
		return new SubUnit(this, with);
	}

	public Unit mul(Unit with) {
		return new MulUnit(this, with);
	}

	public Unit div(Unit with) {
		return new DivUnit(this, with);
	}

	public Unit mod(Unit with) {
		return new ModUnit(this, with);
	}

	public Unit pow(Unit with) {
		return new PowUnit(this, with);
	}

	public Unit min(Unit with) {
		return new MinUnit(this, with);
	}

	public Unit max(Unit with) {
		return new MaxUnit(this, with);
	}

	public Unit shiftLeft(Unit with) {
		return new ShiftLeftUnit(this, with);
	}

	public Unit shiftRight(Unit with) {
		return new ShiftRightUnit(this, with);
	}

	public Unit abs() {
		return new AbsUnit(this);
	}

	public Unit sin() {
		return new SinUnit(this);
	}

	public Unit cos() {
		return new CosUnit(this);
	}

	public Unit tan() {
		return new TanUnit(this);
	}

	public Unit atan() {
		return new AtanUnit(this);
	}

	public Unit atan2(Unit with) {
		return new Atan2Unit(this, with);
	}

	public Unit deg() {
		return new DegUnit(this);
	}

	public Unit rad() {
		return new RadUnit(this);
	}

	public Unit log() {
		return new LogUnit(this);
	}

	public Unit log10() {
		return new Log10Unit(this);
	}

	public Unit log1p() {
		return new Log1pUnit(this);
	}

	public Unit sqrt() {
		return new SqrtUnit(this);
	}

	public Unit sq() {
		return new SqUnit(this);
	}

	public Unit floor() {
		return new FloorUnit(this);
	}

	public Unit ceil() {
		return new CeilUnit(this);
	}

	public Unit not() {
		return new NotUnit(this);
	}

	public Unit and(Unit with) {
		return new AndUnit(this, with);
	}

	public Unit or(Unit with) {
		return new OrUnit(this, with);
	}

	public Unit xor(Unit with) {
		return new XorUnit(this, with);
	}

	public Unit eq(Unit with) {
		return new EqUnit(this, with);
	}

	public Unit neq(Unit with) {
		return new NeqUnit(this, with);
	}

	public Unit gt(Unit with) {
		return new GtUnit(this, with);
	}

	public Unit lt(Unit with) {
		return new LtUnit(this, with);
	}

	public Unit gte(Unit with) {
		return new GteUnit(this, with);
	}

	public Unit lte(Unit with) {
		return new LteUnit(this, with);
	}

	public Unit toBool() {
		return new BoolUnit(this);
	}
}
