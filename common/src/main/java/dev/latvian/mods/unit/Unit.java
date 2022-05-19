package dev.latvian.mods.unit;

import dev.latvian.mods.unit.operator.AddOpUnit;
import dev.latvian.mods.unit.operator.AndOpUnit;
import dev.latvian.mods.unit.operator.BitAndOpUnit;
import dev.latvian.mods.unit.operator.BitNotOpUnit;
import dev.latvian.mods.unit.operator.BitOrOpUnit;
import dev.latvian.mods.unit.operator.BoolNotOpUnit;
import dev.latvian.mods.unit.operator.DivOpUnit;
import dev.latvian.mods.unit.operator.EqOpUnit;
import dev.latvian.mods.unit.operator.GtOpUnit;
import dev.latvian.mods.unit.operator.GteOpUnit;
import dev.latvian.mods.unit.operator.LshOpUnit;
import dev.latvian.mods.unit.operator.LtOpUnit;
import dev.latvian.mods.unit.operator.LteOpUnit;
import dev.latvian.mods.unit.operator.ModOpUnit;
import dev.latvian.mods.unit.operator.MulOpUnit;
import dev.latvian.mods.unit.operator.NegateOpUnit;
import dev.latvian.mods.unit.operator.NeqOpUnit;
import dev.latvian.mods.unit.operator.OrOpUnit;
import dev.latvian.mods.unit.operator.PowOpUnit;
import dev.latvian.mods.unit.operator.RshOpUnit;
import dev.latvian.mods.unit.operator.SubOpUnit;
import dev.latvian.mods.unit.operator.XorOpUnit;

public abstract class Unit {
	public static Unit[] EMPTY_ARRAY = new Unit[0];

	public static Unit of(Object value) {
		if (value instanceof Unit u) {
			return u;
		} else if (value instanceof Number num) {
			return FixedNumberUnit.ofFixed(num.doubleValue());
		} else {
			return UnitContext.DEFAULT.parse(String.valueOf(value));
		}
	}

	public boolean isFixed() {
		return false;
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

	public void toString(StringBuilder builder) {
		builder.append(this);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		toString(builder);
		return builder.toString();
	}

	// Functions

	public Unit positive() {
		return this;
	}

	public Unit negate() {
		return new NegateOpUnit(this);
	}

	public Unit add(Unit other) {
		return new AddOpUnit(this, other);
	}

	public Unit sub(Unit other) {
		return new SubOpUnit(this, other);
	}

	public Unit mul(Unit other) {
		return new MulOpUnit(this, other);
	}

	public Unit div(Unit other) {
		return new DivOpUnit(this, other);
	}

	public Unit mod(Unit other) {
		return new ModOpUnit(this, other);
	}

	public Unit pow(Unit other) {
		return new PowOpUnit(this, other);
	}

	public Unit lsh(Unit other) {
		return new LshOpUnit(this, other);
	}

	public Unit rsh(Unit other) {
		return new RshOpUnit(this, other);
	}

	public Unit bitAnd(Unit other) {
		return new BitAndOpUnit(this, other);
	}

	public Unit bitOr(Unit other) {
		return new BitOrOpUnit(this, other);
	}

	public Unit xor(Unit other) {
		return new XorOpUnit(this, other);
	}

	public Unit bitNot() {
		return new BitNotOpUnit(this);
	}

	public Unit eq(Unit other) {
		return new EqOpUnit(this, other);
	}

	public Unit neq(Unit other) {
		return new NeqOpUnit(this, other);
	}

	public Unit lt(Unit other) {
		return new LtOpUnit(this, other);
	}

	public Unit gt(Unit other) {
		return new GtOpUnit(this, other);
	}

	public Unit lte(Unit other) {
		return new LteOpUnit(this, other);
	}

	public Unit gte(Unit other) {
		return new GteOpUnit(this, other);
	}

	public Unit and(Unit other) {
		return new AndOpUnit(this, other);
	}

	public Unit or(Unit other) {
		return new OrOpUnit(this, other);
	}

	public Unit boolNot() {
		return new BoolNotOpUnit(this);
	}
}
