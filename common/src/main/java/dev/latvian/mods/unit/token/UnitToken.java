package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;

import java.util.Stack;

public interface UnitToken {
	default Unit interpret(UnitTokenStream stream) {
		return (Unit) this;
	}

	default boolean shouldNegate() {
		return false;
	}

	default void unstack(UnitTokenStream stream, Stack<UnitToken> resultStack) {
		resultStack.push(this);
	}
}
