package dev.latvian.mods.unit.token;

public record PositionedUnitToken(UnitToken token, int position) {
	public static final PositionedUnitToken[] EMPTY = new PositionedUnitToken[0];

	@Override
	public String toString() {
		return token.toString();
	}
}
