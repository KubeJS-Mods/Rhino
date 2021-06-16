package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.HideFromJS;

public class Rect {
	public final int width;
	public final transient int height;

	@HideFromJS
	public Rect(int w, int h) {
		width = w;
		height = h;
	}

	public Rect(int w, int h, int d) {
		this(w, h);
		System.out.println("Depth: " + d);
	}
}