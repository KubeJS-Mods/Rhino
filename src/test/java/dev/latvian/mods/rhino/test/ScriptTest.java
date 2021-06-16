package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.ClassShutter;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.util.DynamicFunction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class ScriptTest {
	public static void main(String[] args) {
		EventsJS eventsJS = new EventsJS();

		try {
			Context cx = Context.enter();

			cx.setClassShutter((fullClassName, type) -> {
				System.out.println("-- Checking class permissions " + fullClassName + " / " + type);
				return type != ClassShutter.TYPE_CLASS_IN_PACKAGE || !fullClassName.startsWith("java.net");
			});

			cx.getTypeWrappers().register(ResourceLocation.class, o -> !"rhino:array_test_1".equals(o), ResourceLocation::new);

			Scriptable scope = cx.initStandardObjects();

			ScriptableObject.putProperty(scope, "console", Context.javaToJS(new ConsoleJS(), scope));

			ScriptableObject.putProperty(scope, "newMath", Context.javaToJS(new NativeJavaClass(scope, Math.class), scope));
			ScriptableObject.putProperty(scope, "Rect", new NativeJavaClass(scope, Rect.class));
			ScriptableObject.putProperty(scope, "ResourceLocation", new NativeJavaClass(scope, ResourceLocation.class));

			ScriptableObject.putProperty(scope, "events", Context.javaToJS(eventsJS, scope));
			ScriptableObject.putProperty(scope, "sqTest", Context.javaToJS(new DynamicFunction(o -> ((Number) o[0]).doubleValue() * ((Number) o[0]).doubleValue()), scope));

			loadTest(cx, scope, "rhino_test_script.js");
		} catch (RhinoException ex) {
			StringBuilder sb = new StringBuilder("Script error in ");
			sb.append(ex.sourceName());
			sb.append(':');
			sb.append(ex.lineNumber());

			if (ex.columnNumber() > 0) {
				sb.append(':');
				sb.append(ex.columnNumber());
			}

			sb.append(": ");
			sb.append(ex.details());
			System.err.println(sb);

			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			Context.exit();
		}

		for (Consumer<Object> consumer : eventsJS.lastCallback) {
			consumer.accept(48);
		}
	}

	private static void loadTest(Context cx, Scriptable scope, String name) throws Exception {
		try (Reader reader = new BufferedReader(new InputStreamReader(ScriptTest.class.getResourceAsStream("/tests/" + name), StandardCharsets.UTF_8))) {
			cx.evaluateReader(scope, reader, name, 1, null);
		}
	}
}
