package dev.latvian.mods.rhino.mod.util.color;

import net.minecraft.network.chat.TextColor;

public class SimpleColorWithAlpha implements Color {
	private final int value;
	private TextColor textColor;

	public SimpleColorWithAlpha(int v) {
		value = v;
	}

	@Override
	public int getArgbKJS() {
		return value;
	}

	@Override
	public TextColor createTextColorKJS() {
		if (textColor == null) {
			textColor = TextColor.fromRgb(getRgbKJS());
		}

		return textColor;
	}

	@Override
	public String toString() {
		return getHexKJS();
	}
}