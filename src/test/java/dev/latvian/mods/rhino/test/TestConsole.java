package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.latvian.mods.unit.UnitContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RemapPrefixForJS("test1$")
@RemapPrefixForJS("test2$")
public class TestConsole {
	private final ContextFactory factory;
	private TestConsoleTheme theme;
	public StringBuilder consoleOutput = new StringBuilder();

	public TestConsole(ContextFactory factory) {
		this.factory = factory;
	}

	public void info(Object o) {
		String s = String.valueOf(o);

		StringBuilder builder = new StringBuilder();

		var lineP = new int[]{0};
		var lineS = Context.getSourcePositionFromStack(factory.enter(), lineP);

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

	public String getConsoleOutput() {
		String s = consoleOutput.toString();
		consoleOutput.setLength(0);
		return s;
	}

	public void freeze(Object... objects) {
		System.out.println("Freezing " + Arrays.toString(objects));
	}

	public String[] getTestArray() {
		return new String[]{"abc", "def", "ghi"};
	}

	public List<String> getTestList() {
		return new ArrayList<>(Arrays.asList(getTestArray()));
	}

	public void test1$setTheme(TestConsoleTheme t) {
		info("Set theme to " + t);
		theme = t;
	}

	public TestConsoleTheme test2$getTheme() {
		return theme;
	}

	public void printUnit(String input) {
		info(input + " -> " + UnitContext.DEFAULT.parse(input));
	}

	public void printMaterial(TestMaterial material) {
		info("%s#%08x".formatted(material.name(), material.hashCode()));
	}
}
