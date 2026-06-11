package dev.latvian.mods.rhino.test;

import java.util.TreeMap;
import java.util.UUID;

public class StaticUtils {
	public static final int immutableInt = 40;
	public static int mutableInt = 20;

	public static void test(TestConsole console) {
		console.info("hi");
	}

	public interface Defaulted {
		default String someMethod() {
			return "default";
		}
	}

	public interface DefaultedAbstract {
		String abstractMethod();

		default String someMethod() {
			return "default";
		}
	}

	public static String callDefaulted(Defaulted d) {
		return d.someMethod();
	}

	public static String callDefaultedAbstract(DefaultedAbstract d) {
		return d.someMethod() + "," + d.abstractMethod();
	}

	public static UUID uuid(String s) {
		return UUID.fromString(s);
	}

	public static TreeMap<UUID, String> treeMapWithUuidKeys() {
		TreeMap<UUID, String> map = new TreeMap<>();
		map.put(UUID.fromString("d2a4e51b-8c6a-4dbe-b8c2-67a8a9c81e25"), "existing");
		return map;
	}
}
