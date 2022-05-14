package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;

public interface UnitToken {
	default boolean shouldNegate() {
		return false;
	}

	Unit interpret(UnitContext context);

	default UnitToken negate() {
		return new NegateUnitToken(this);
	}

	default UnitToken bitNot() {
		return this;
	}
}
