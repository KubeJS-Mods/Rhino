package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.mod.util.CollectionTagWrapper;
import dev.latvian.mods.rhino.mod.util.CompoundTagWrapper;
import dev.latvian.mods.rhino.mod.util.NBTUtils;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Assertions;

public class RhinoTest {
	public final String testName;
	public Scriptable sharedScope;
	public boolean shareScope;

	public RhinoTest(String n) {
		testName = n;
	}

	public RhinoTest shareScope() {
		shareScope = true;
		return this;
	}

	public Scriptable createScope(Context cx) {
		if (sharedScope != null) {
			cx.sharedContextData = SharedContextData.get(sharedScope);
			return sharedScope;
		}

		var scope = cx.initStandardObjects();
		var data = SharedContextData.get(cx, scope);

		var typeWrappers = data.getTypeWrappers();
		typeWrappers.register(CompoundTag.class, NBTUtils::isTagCompound, NBTUtils::toTagCompound);
		typeWrappers.register(CollectionTag.class, NBTUtils::isTagCollection, NBTUtils::toTagCollection);
		typeWrappers.register(ListTag.class, NBTUtils::isTagCollection, NBTUtils::toTagList);
		typeWrappers.register(Tag.class, NBTUtils::toTag);

		data.addCustomJavaToJsWrapper(CompoundTag.class, CompoundTagWrapper::new);
		data.addCustomJavaToJsWrapper(CollectionTag.class, CollectionTagWrapper::new);

		registerData(data);

		if (shareScope) {
			sharedScope = scope;
		}

		return scope;
	}

	public void registerData(SharedContextData data) {
		data.addToTopLevelScope("console", TestConsole.class);
		data.addToTopLevelScope("NBT", NBTUtils.class);
	}

	public void test(String name, String script, String console) {
		var cx = Context.enterWithNewFactory();

		try {
			var scope = createScope(cx);
			cx.evaluateString(scope, script, testName + "/" + name, 1, null);
		} catch (Exception ex) {
			ex.printStackTrace();
			TestConsole.info("Error: " + ex.getMessage());
			// ex.printStackTrace();
		} finally {
			Context.exit();
		}

		Assertions.assertEquals(console.trim(), TestConsole.getConsoleOutput().trim());
	}
}
