package dev.latvian.mods.rhino.mod.test;

import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;

public class TestConsole {
	@HideFromJS
	public int consoleTest = 304;

	@RemapForJS("consoleTest")
	public int consoleTest123 = 305;

	public void info(Object o) {
		System.out.println(o);
	}

	public void infoClass(Object o) {
		System.out.println(o + " / " + o.getClass().getName());
	}
}