package dev.latvian.mods.rhino.mod.util.color;

import net.minecraft.network.chat.TextColor;

public final class NoColor implements Color {
	private static final TextColor TEXT_COLOR = TextColor.fromRgb(0);

	@Override
	public int getArgbKJS() {
		return 0;
	}

	@Override
	public int getRgbKJS() {
		return 0;
	}

	@Override
	public String getHexKJS() {
		return "#00000000";
	}

	@Override
	public String getSerializeKJS() {
		return "none";
	}

	@Override
	public TextColor createTextColorKJS() {
		return TEXT_COLOR;
	}
}
