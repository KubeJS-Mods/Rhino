package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class HsvFuncUnit extends FuncUnit {
	public final Unit h, s, v, a;
	public static final FunctionFactory FACTORY = FunctionFactory.of("hsv", 3, 4, HsvFuncUnit::new);

	public HsvFuncUnit(Unit[] args) {
		super(FACTORY);
		h = args[0];
		s = args[1];
		v = args[2];
		a = args.length == 4 ? args[3] : FixedNumberUnit.ONE;
	}

	@Override
	protected Unit[] getArguments() {
		if (a == FixedNumberUnit.ONE) {
			return new Unit[]{h, s, v};
		}

		return new Unit[]{h, s, v, a};
	}

	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		double h = this.h.get(variables);
		double s = this.s.get(variables);
		double v = this.v.get(variables);

		int i = (int) (h * 6D) % 6;
		double j = h * 6D - (float) i;
		double k = v * (1D - s);
		double l = v * (1D - j * s);
		double m = v * (1D - (1D - j) * s);
		double dr;
		double dg;
		double db;
		switch (i) {
			case 0 -> {
				dr = v;
				dg = m;
				db = k;
			}
			case 1 -> {
				dr = l;
				dg = v;
				db = k;
			}
			case 2 -> {
				dr = k;
				dg = v;
				db = m;
			}
			case 3 -> {
				dr = k;
				dg = l;
				db = v;
			}
			case 4 -> {
				dr = m;
				dg = k;
				db = v;
			}
			case 5 -> {
				dr = v;
				dg = k;
				db = l;
			}
			default -> {
				dr = 0D;
				dg = 0D;
				db = 0D;
			}
		}

		int cr = (int) (ClampFuncUnit.clamp(dr * 255D, 0D, 255D));
		int cg = (int) (ClampFuncUnit.clamp(dg * 255D, 0D, 255D));
		int cb = (int) (ClampFuncUnit.clamp(db * 255D, 0D, 255D));
		int ca = (int) (ClampFuncUnit.clamp(this.a.get(variables) * 255D, 0D, 255D));
		return ca << 24 | cr << 16 | cg << 8 | cb;
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return a.getBoolean(variables);
	}

	@Override
	public Unit withAlpha(Unit a) {
		if (a == this.a) {
			return this;
		}

		return new HsvFuncUnit(new Unit[]{h, s, v, a});
	}


}
