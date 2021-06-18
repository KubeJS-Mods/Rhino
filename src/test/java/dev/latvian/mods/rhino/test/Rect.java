package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;

public class Rect {
	public final int width;
	public final transient int height;

	public static String rectId = "3adwwad";

	@HideFromJS
	public Rect(int w, int h) {
		width = w;
		height = h;
	}

	public Rect(int w, int h, int d) {
		this(w, h);
		System.out.println("Depth: " + d);
	}

	@RemapForJS("createRect")
	public static Rect construct(int w, int h) {
		return new Rect(w, h);
	}
}