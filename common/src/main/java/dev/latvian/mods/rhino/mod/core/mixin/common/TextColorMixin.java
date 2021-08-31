package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.mod.util.color.Color;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextColor.class)
public abstract class TextColorMixin implements Color {
	@Shadow
	@Final
	private int value;

	@Override
	public int getArgbKJS() {
		return 0xFF000000 | value;
	}

	@Override
	public int getRgbKJS() {
		return value;
	}

	@Override
	public String getSerializeKJS() {
		return serialize();
	}

	@Shadow
	public abstract String serialize();
}
