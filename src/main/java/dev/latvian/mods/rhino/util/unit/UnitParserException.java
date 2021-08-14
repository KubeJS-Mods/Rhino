package dev.latvian.mods.rhino.util.unit;

public class UnitParserException extends IllegalArgumentException {
	public UnitParserException(String string, int startPos, String reason) {
		super("Failed to parse unit string '" + string + "' at " + startPos + ": " + reason);
	}
}
