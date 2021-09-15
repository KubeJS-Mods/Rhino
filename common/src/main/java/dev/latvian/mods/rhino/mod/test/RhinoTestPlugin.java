package dev.latvian.mods.rhino.mod.test;

import dev.latvian.kubejs.KubeJSPlugin;
import dev.latvian.kubejs.script.BindingsEvent;
import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import me.shedaniel.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;

public class RhinoTestPlugin extends KubeJSPlugin {
	@Override
	public void addBindings(BindingsEvent event) {
		if (Platform.isDevelopmentEnvironment()) {
			event.add("TestConsole", new TestConsole());
			event.add("JavaMath", Math.class);
			event.add("Rect", Rect.class);
			event.add("ResourceLocation", ResourceLocation.class);

			event.add("RhinoTest", new RhinoTest());
			event.addFunction("sqTest", o -> ((Number) o[0]).doubleValue() * ((Number) o[0]).doubleValue());

			event.add("NBTTest", NBTWrapper.class);
		}
	}
}
