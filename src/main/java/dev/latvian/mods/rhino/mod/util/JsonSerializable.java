package dev.latvian.mods.rhino.mod.util;

import com.google.gson.JsonElement;
import dev.latvian.mods.rhino.util.RemapForJS;

/**
 * @author LatvianModder
 */
public interface JsonSerializable {
	@RemapForJS("toJson")
	JsonElement toJsonJS();
}