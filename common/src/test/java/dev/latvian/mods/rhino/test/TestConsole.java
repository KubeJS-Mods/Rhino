package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.unit.UnitContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestConsole {
	private static ConsoleTheme theme;
	public static StringBuilder consoleOutput = new StringBuilder();

	public static void info(Object o) {
		String s = String.valueOf(o);

		StringBuilder builder = new StringBuilder();

		var lineP = new int[]{0};
		var lineS = Context.getSourcePositionFromStack(lineP);

		if (lineP[0] > 0) {
			if (lineS != null) {
				builder.append(lineS);
			}

			builder.append(':');
			builder.append(lineP[0]);
			builder.append(": ");
		}

		builder.append(s);

		System.out.println(builder);

		if (consoleOutput.length() > 0) {
			consoleOutput.append('\n');
		}

		consoleOutput.append(s);
	}

	public static String getConsoleOutput() {
		String s = consoleOutput.toString();
		consoleOutput.setLength(0);
		return s;
	}

	public static void freeze(Object... objects) {
		System.out.println("Freezing " + Arrays.toString(objects));
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

	public static void printUnit(String input) {
		System.out.println("# " + input);
		System.out.println("> " + UnitContext.DEFAULT.parse(input));
	}
}
