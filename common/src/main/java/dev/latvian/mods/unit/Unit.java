package dev.latvian.mods.unit;

import dev.latvian.mods.unit.function.AbsFuncUnit;
import dev.latvian.mods.unit.function.Atan2FuncUnit;
import dev.latvian.mods.unit.function.AtanFuncUnit;
import dev.latvian.mods.unit.function.BoolFuncUnit;
import dev.latvian.mods.unit.function.CeilFuncUnit;
import dev.latvian.mods.unit.function.ClampFuncUnit;
import dev.latvian.mods.unit.function.CosFuncUnit;
import dev.latvian.mods.unit.function.DegFuncUnit;
import dev.latvian.mods.unit.function.FloorFuncUnit;
import dev.latvian.mods.unit.function.LerpFuncUnit;
import dev.latvian.mods.unit.function.Log10FuncUnit;
import dev.latvian.mods.unit.function.Log1pFuncUnit;
import dev.latvian.mods.unit.function.LogFuncUnit;
import dev.latvian.mods.unit.function.MaxFuncUnit;
import dev.latvian.mods.unit.function.MinFuncUnit;
import dev.latvian.mods.unit.function.RadFuncUnit;
import dev.latvian.mods.unit.function.SinFuncUnit;
import dev.latvian.mods.unit.function.SmoothstepFuncUnit;
import dev.latvian.mods.unit.function.SqFuncUnit;
import dev.latvian.mods.unit.function.SqrtFuncUnit;
import dev.latvian.mods.unit.function.TanFuncUnit;
import dev.latvian.mods.unit.function.WithAlphaFuncUnit;
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

	// Operators

	public Unit positive() {
		return this;
	}

	public Unit negate() {
		return new NegateOpUnit(this);
	}

	public Unit add(Unit other) {
		return new AddOpUnit(this, other);
	}

	public Unit add(double value) {
		return add(FixedNumberUnit.of(value));
	}

	public Unit sub(Unit other) {
		return new SubOpUnit(this, other);
	}

	public Unit sub(double value) {
		return sub(FixedNumberUnit.of(value));
	}

	public Unit mul(Unit other) {
		return new MulOpUnit(this, other);
	}

	public Unit mul(double value) {
		return add(FixedNumberUnit.of(value));
	}

	public Unit div(Unit other) {
		return new DivOpUnit(this, other);
	}

	public Unit div(double value) {
		return add(FixedNumberUnit.of(value));
	}

	public Unit mod(Unit other) {
		return new ModOpUnit(this, other);
	}

	public Unit mod(double value) {
		return mod(FixedNumberUnit.of(value));
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

	// Functions

	public Unit min(Unit other) {
		return new MinFuncUnit(this, other);
	}

	public Unit max(Unit other) {
		return new MaxFuncUnit(this, other);
	}

	public Unit abs() {
		return new AbsFuncUnit(this);
	}

	public Unit sin() {
		return new SinFuncUnit(this);
	}

	public Unit cos() {
		return new CosFuncUnit(this);
	}

	public Unit tan() {
		return new TanFuncUnit(this);
	}

	public Unit deg() {
		return new DegFuncUnit(this);
	}

	public Unit rad() {
		return new RadFuncUnit(this);
	}

	public Unit atan() {
		return new AtanFuncUnit(this);
	}

	public Unit atan2(Unit other) {
		return new Atan2FuncUnit(this, other);
	}

	public Unit log() {
		return new LogFuncUnit(this);
	}

	public Unit log10() {
		return new Log10FuncUnit(this);
	}

	public Unit log1p() {
		return new Log1pFuncUnit(this);
	}

	public Unit sqrt() {
		return new SqrtFuncUnit(this);
	}

	public Unit sq() {
		return new SqFuncUnit(this);
	}

	public Unit floor() {
		return new FloorFuncUnit(this);
	}

	public Unit ceil() {
		return new CeilFuncUnit(this);
	}

	public Unit bool() {
		return new BoolFuncUnit(this);
	}

	public Unit clamp(Unit a, Unit b) {
		return new ClampFuncUnit(this, a, b);
	}

	public Unit lerp(Unit a, Unit b) {
		return new LerpFuncUnit(this, a, b);
	}

	public Unit smoothstep() {
		return new SmoothstepFuncUnit(this);
	}

	public Unit withAlpha(Unit a) {
		return new WithAlphaFuncUnit(this, a);
	}
}
