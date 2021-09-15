package dev.latvian.mods.rhino.mod.test;

import dev.latvian.mods.rhino.util.MapLike;

public class TestMapLike implements MapLike<String, String> {
	@Override
	public String getML(String key) {
		switch (key) {
			case "a":
				return "abc";
			case "b":
				return "def";
		}

		return null;
	}
}
