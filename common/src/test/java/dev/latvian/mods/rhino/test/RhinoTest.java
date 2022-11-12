package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeObject;
import dev.latvian.mods.rhino.Scriptable;
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
	public final Context context;
	public final Scriptable rootScope;
	public boolean shareScope;
	public TestConsole console;

	public RhinoTest(String n) {
		testName = n;
		context = Context.enter();
		console = new TestConsole(context);

		var typeWrappers = context.sharedContextData.getTypeWrappers();
		typeWrappers.register(CompoundTag.class, NBTUtils::isTagCompound, NBTUtils::toTagCompound);
		typeWrappers.register(CollectionTag.class, NBTUtils::isTagCollection, NBTUtils::toTagCollection);
		typeWrappers.register(ListTag.class, NBTUtils::isTagCollection, NBTUtils::toTagList);
		typeWrappers.register(Tag.class, NBTUtils::toTag);
		typeWrappers.register(TestMaterial.class, TestMaterial::get);

		context.sharedContextData.addCustomJavaToJsWrapper(CompoundTag.class, CompoundTagWrapper::new);
		context.sharedContextData.addCustomJavaToJsWrapper(CollectionTag.class, CollectionTagWrapper::new);

		rootScope = context.initStandardObjects();
		context.addToScope(rootScope, "console", console);
		context.addToScope(rootScope, "NBT", NBTUtils.class);
	}

	public RhinoTest shareScope() {
		shareScope = true;
		return this;
	}

	public void test(String name, String script, String match) {
		try {
			Scriptable scope;

			if (shareScope) {
				scope = rootScope;
			} else {
				scope = new NativeObject(context);
				scope.setParentScope(rootScope);
			}

			context.evaluateString(scope, script, testName + "/" + name, 1, null);
		} catch (Exception ex) {
			ex.printStackTrace();
			console.info("Error: " + ex.getMessage());
			// ex.printStackTrace();
		}

		Assertions.assertEquals(match.trim(), console.getConsoleOutput().trim());
	}
}
