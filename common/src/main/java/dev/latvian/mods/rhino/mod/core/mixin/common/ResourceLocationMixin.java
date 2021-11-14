package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.util.RemapForJS;
import dev.latvian.mods.rhino.util.SpecialEquality;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ResourceLocation.class, priority = 1001)
public abstract class ResourceLocationMixin implements SpecialEquality {
	@Shadow
	@RemapForJS("getNamespace")
	public abstract String getNamespace();

	@Shadow
	@RemapForJS("getPath")
	public abstract String getPath();

	@Override
	public boolean specialEquals(Object o, boolean shallow) {
		return equals(o instanceof ResourceLocation ? o : toString().equals(String.valueOf(o)));
	}
}
