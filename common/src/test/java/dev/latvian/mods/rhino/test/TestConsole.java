package dev.latvian.mods.rhino.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestConsole {
	public static void info(Object o) {
		System.out.println(o);
	}

	public static void freeze(Object[] objects) {
		System.out.println("Freeze");
	}

	public static String[] getTestArray() {
		return new String[]{"abc", "def", "ghi"};
	}

	public static List<String> getTestList() {
		return new ArrayList<>(Arrays.asList(getTestArray()));
	}
}
