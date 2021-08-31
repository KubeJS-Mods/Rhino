package dev.latvian.mods.rhino.mod.util.color;

import dev.latvian.mods.rhino.mod.wrapper.ColorWrapper;
import dev.latvian.mods.rhino.util.SpecialEquality;
import net.minecraft.network.chat.TextColor;

public interface Color extends SpecialEquality {
	int getArgbKJS();

	default int getRgbKJS() {
		return getArgbKJS() & 0xFFFFFF;
	}

	default int getFireworkColorKJS() {
		return getRgbKJS();
	}

	default String getHexKJS() {
		return String.format("#%08X", getArgbKJS());
	}

	default String getSerializeKJS() {
		return getHexKJS();
	}

	default TextColor createTextColorKJS() {
		return TextColor.fromRgb(getRgbKJS());
	}

	@Override
	default boolean specialEquals(Object o, boolean shallow) {
		Color c = ColorWrapper.of(o);
		return shallow ? (getArgbKJS() == c.getArgbKJS()) : (getRgbKJS() == c.getRgbKJS());
	}
}