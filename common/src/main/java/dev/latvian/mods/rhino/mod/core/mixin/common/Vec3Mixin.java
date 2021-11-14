package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author LatvianModder
 */
@Mixin(value = Vec3.class, priority = 1001)
public abstract class Vec3Mixin {
	@Shadow
	@Final
	@RemapForJS("x")
	public double x;

	@Shadow
	@Final
	@RemapForJS("y")
	public double y;

	@Shadow
	@Final
	@RemapForJS("z")
	public double z;
}
