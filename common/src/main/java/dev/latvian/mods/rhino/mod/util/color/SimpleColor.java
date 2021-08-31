package dev.latvian.mods.rhino.mod.util.color;

import net.minecraft.network.chat.TextColor;

public class SimpleColor implements Color {
	public static final SimpleColor BLACK = new SimpleColor(0xFF000000);
	public static final SimpleColor WHITE = new SimpleColor(0xFFFFFFFF);

	private final int value;
	private TextColor textColor;

	public SimpleColor(int v) {
		value = 0xFF000000 | v;
	}

	@Override
	public int getArgbKJS() {
		return value;
	}

	@Override
	public String getHexKJS() {
		return String.format("#%06X", getRgbKJS());
	}

	@Override
	public String toString() {
		return getHexKJS();
	}

	@Override
	public TextColor createTextColorKJS() {
		if (textColor == null) {
			textColor = TextColor.fromRgb(getRgbKJS());
		}

		return textColor;
	}
}