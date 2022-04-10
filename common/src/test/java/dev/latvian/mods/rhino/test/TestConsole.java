package dev.latvian.mods.rhino.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestConsole {
	private static ConsoleTheme theme;

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

	public static void setTheme(ConsoleTheme t) {
		System.out.println("Set theme to " + t);
		theme = t;
	}

	public static ConsoleTheme getTheme() {
		return theme;
	}
}
