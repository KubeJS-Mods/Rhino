package dev.latvian.mods.rhino.util.unit;

@FunctionalInterface
public interface Unit {
	Unit ZERO = new FixedUnit(0F);
	Unit ONE = new FixedUnit(1F);
	Unit PI = new FixedUnit(3.141592653589793F);
	Unit E = new FixedUnit(2.718281828459045F);

	static Unit fixed(float value) {
		return value == 0F ? ZERO : value == 1F ? ONE : new FixedUnit(value);
	}

	static Unit parse(String string, UnitVariables variables) {
		return new UnitParser(string, variables).parse();
	}

	float get();

	default Unit neg() {
		return new NegUnit(this);
	}

	default Unit add(Unit with) {
		return new SumUnit(this, with);
	}

	default Unit sub(Unit with) {
		return new SubUnit(this, with);
	}

	default Unit mul(Unit with) {
		return new MulUnit(this, with);
	}

	default Unit div(Unit with) {
		return new DivUnit(this, with);
	}

	default Unit mod(Unit with) {
		return new ModUnit(this, with);
	}

	default Unit pow(Unit with) {
		return new PowUnit(this, with);
	}

	default Unit min(Unit with) {
		return new MinUnit(this, with);
	}

	default Unit max(Unit with) {
		return new MaxUnit(this, with);
	}

	default Unit shiftLeft(Unit with) {
		return new ShiftLeftUnit(this, with);
	}

	default Unit shiftRight(Unit with) {
		return new ShiftRightUnit(this, with);
	}

	default Unit abs() {
		return new AbsUnit(this);
	}

	default Unit sin() {
		return new SinUnit(this);
	}

	default Unit cos() {
		return new CosUnit(this);
	}

	default Unit tan() {
		return new TanUnit(this);
	}

	default Unit atan() {
		return new AtanUnit(this);
	}

	default Unit atan2(Unit with) {
		return new Atan2Unit(this, with);
	}

	default Unit deg() {
		return new DegUnit(this);
	}

	default Unit rad() {
		return new RadUnit(this);
	}

	default Unit log() {
		return new LogUnit(this);
	}

	default Unit log10() {
		return new Log10Unit(this);
	}

	default Unit log1p() {
		return new Log1pUnit(this);
	}

	default Unit sqrt() {
		return new SqrtUnit(this);
	}

	default Unit sq() {
		return new SqUnit(this);
	}

	default Unit floor() {
		return new FloorUnit(this);
	}

	default Unit ceil() {
		return new CeilUnit(this);
	}
}
