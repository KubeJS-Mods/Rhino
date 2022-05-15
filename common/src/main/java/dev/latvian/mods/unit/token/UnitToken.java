package dev.latvian.mods.unit.token;

public interface UnitToken {
	default boolean shouldNegate() {
		return false;
	}
}
