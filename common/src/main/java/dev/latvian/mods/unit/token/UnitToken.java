package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;

public interface UnitToken {
	default boolean shouldNegate() {
		return false;
	}

	default Unit interpret(UnitTokenStream stream) {
		if (this instanceof Unit u) {
			return u;
		}

		throw new IllegalStateException("This UnitToken can't be parsed!");
	}
}
