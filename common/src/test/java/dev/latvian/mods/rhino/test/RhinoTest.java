package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.mod.util.NBTUtils;
import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;

public class RhinoTest {
	public static void main(String[] args) {
		var context = Context.enterWithNewFactory();
		// context.setClassShutter((fullClassName, type) -> type != ClassShutter.TYPE_CLASS_IN_PACKAGE || isClassAllowed(fullClassName));

		RhinoTest test = new RhinoTest(context);
		test.add("console", TestConsole.class);
		test.add("NBT", NBTWrapper.class);

		var typeWrappers = context.getTypeWrappers();
		typeWrappers.register(CompoundTag.class, o -> (CompoundTag) NBTUtils.toNBT(o));

		test.load("/rhinotest/test.js");
		// test.load("/rhinotest/nbt.js");
		test.load("/rhinotest/nullish_coalescing.js");
		test.load("/rhinotest/pow.js");
	}

	public final Context context;
	public final ScriptableObject scope;

	public RhinoTest(Context c) {
		context = c;
		scope = context.initStandardObjects();
	}

	public void add(String name, Object value) {
		if (value.getClass() == Class.class) {
			ScriptableObject.putProperty(scope, name, new NativeJavaClass(scope, (Class<?>) value));
		} else {
			ScriptableObject.putProperty(scope, name, Context.javaToJS(value, scope));
		}
	}

	public void load(String file) {
		try (var stream = RhinoTest.class.getResourceAsStream(file)) {
			var script = new String(IOUtils.toByteArray(new BufferedInputStream(stream)), StandardCharsets.UTF_8);
			context.evaluateString(scope, script, file, 1, null);
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}
}
