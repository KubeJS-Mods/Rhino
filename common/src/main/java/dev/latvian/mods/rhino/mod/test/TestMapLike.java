package dev.latvian.mods.rhino.mod.test;

import dev.latvian.mods.rhino.util.MapLike;

public class TestMapLike implements MapLike<String, String> {
	@Override
	public String getML(String key) {
		return switch (key) {
			case "a" -> "abc";
			case "b" -> "def";
			default -> null;
		};

	}
}
