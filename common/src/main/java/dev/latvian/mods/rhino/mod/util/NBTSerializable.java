package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.nbt.Tag;

/**
 * @author LatvianModder
 */
public interface NBTSerializable {
	@RemapForJS("toNBT")
	Tag toNBTJS();
}