package dev.latvian.mods.rhino.util.unit;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class UnitVariables {
	private final Map<String, Unit> map = new HashMap<>();

	public void clear() {
		map.clear();
	}

	public void set(String key, Unit unit) {
		map.put(key, unit);
	}

	@Nullable
	public Unit get(String key) {
		return map.get(key);
	}
}
