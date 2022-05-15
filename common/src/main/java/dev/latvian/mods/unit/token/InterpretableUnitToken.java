package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;

public interface InterpretableUnitToken extends UnitToken {
	default Unit interpret(UnitContext context) {
		return (Unit) this;
	}

	default InterpretableUnitToken negate() {
		return new NegateUnitToken(this);
	}

	default InterpretableUnitToken bitNot() {
		return new BitNotUnitToken(this);
	}

	default InterpretableUnitToken negateAndBitNot(boolean negate, boolean bitNot) {
		InterpretableUnitToken t = this;

		if (negate) {
			t = t.negate();
		}

		if (bitNot) {
			t = t.bitNot();
		}

		return t;
	}
}
