package dev.latvian.mods.unit.token;

import org.jetbrains.annotations.Nullable;

public record PositionedUnitToken(UnitToken token, int position, @Nullable PositionedUnitToken prev) {
	public static final PositionedUnitToken[] EMPTY = new PositionedUnitToken[0];

	@Override
	public String toString() {
		return token.toString();
	}
}
