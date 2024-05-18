package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.util.RemappedEnumConstant;
import net.minecraft.util.StringRepresentable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StringRepresentable.class)
public interface StringRepresentableMixin extends RemappedEnumConstant {
	@Override
	default String getRemappedEnumConstantName() {
		return ((StringRepresentable) this).getSerializedName();
	}
}
