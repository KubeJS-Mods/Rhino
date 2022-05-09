package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.mod.util.CollectionTagWrapper;
import dev.latvian.mods.rhino.mod.util.CompoundTagWrapper;
import dev.latvian.mods.rhino.mod.util.NBTUtils;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

public class RhinoTest {
	private static Context context;

	public static Context getContext() {
		if (context != null) {
			return context;
		}

		context = Context.enterWithNewFactory();
		// context.setClassShutter((fullClassName, type) -> type != ClassShutter.TYPE_CLASS_IN_PACKAGE || isClassAllowed(fullClassName));

		var typeWrappers = context.getTypeWrappers();
		typeWrappers.register(CompoundTag.class, NBTUtils::isTagCompound, NBTUtils::toTagCompound);
		typeWrappers.register(CollectionTag.class, NBTUtils::isTagCollection, NBTUtils::toTagCollection);
		typeWrappers.register(ListTag.class, NBTUtils::isTagCollection, NBTUtils::toTagList);
		typeWrappers.register(Tag.class, NBTUtils::toTag);

		context.addCustomJavaToJsWrapper(CompoundTag.class, CompoundTagWrapper::new);
		context.addCustomJavaToJsWrapper(CollectionTag.class, CollectionTagWrapper::new);

		return context;
	}

	public final String testName;
	public final Map<String, Object> include;

	public RhinoTest(String n) {
		testName = n;
		include = new HashMap<>();
		add("console", TestConsole.class);
		add("NBT", NBTUtils.class);
	}

	public RhinoTest add(String name, Object value) {
		include.put(name, value);
		return this;
	}

	public void test(String name, String script, String console) {
		var scope = getContext().initStandardObjects();

		for (var entry : include.entrySet()) {
			if (entry.getValue() instanceof Class<?> c) {
				ScriptableObject.putProperty(scope, entry.getKey(), new NativeJavaClass(scope, c));
			} else {
				ScriptableObject.putProperty(scope, entry.getKey(), Context.javaToJS(entry.getValue(), scope));
			}
		}

		getContext().evaluateString(scope, script, testName + "/" + name, 1, null);
		Assertions.assertEquals(console.trim(), TestConsole.getConsoleOutput().trim());
	}
}
