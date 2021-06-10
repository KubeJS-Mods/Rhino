package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.ClassShutter;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.util.DataObject;
import dev.latvian.mods.rhino.util.DynamicFunction;
import dev.latvian.mods.rhino.util.DynamicMap;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class ScriptTest {
	public static void main(String[] args) {
		EventsJS eventsJS = new EventsJS();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ScriptTest.class.getResourceAsStream("/rhino_test_script.js"), StandardCharsets.UTF_8))) {
			Context cx = Context.enter();
			cx.setLanguageVersion(Context.VERSION_ES6);

			cx.setClassShutter((fullClassName, type) -> {
				System.out.println("-- Checking class permissions " + fullClassName + " / " + type);
				return type != ClassShutter.TYPE_CLASS_IN_PACKAGE || !fullClassName.startsWith("java.net");
			});

			cx.getTypeWrappers().register(Identifier.class, o -> !"rhino:array_test_1".equals(o), Identifier::new);

			Scriptable scope = cx.initStandardObjects();

			ScriptableObject.putProperty(scope, "console", Context.javaToJS(new ConsoleJS(), scope));

			ScriptableObject.putProperty(scope, "newMath", Context.javaToJS(new NativeJavaClass(scope, Math.class), scope));
			ScriptableObject.putProperty(scope, "Rect", new NativeJavaClass(scope, Rect.class));

			ScriptableObject.putProperty(scope, "events", Context.javaToJS(eventsJS, scope));
			ScriptableObject.putProperty(scope, "sqTest", Context.javaToJS(new DynamicFunction(o -> ((Number) o[0]).doubleValue() * ((Number) o[0]).doubleValue()), scope));

			cx.evaluateReader(scope, reader, "rhino_test_script.js", 1, null);
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

		eventsJS.lastCallback.accept(48);
	}

	public static class ConsoleJS {
		@HideFromJS
		public int consoleTest = 304;

		@RemapForJS("consoleTest")
		public int consoleTest123 = 305;

		public void info(Object o) {
			System.out.println(o);
		}

		public void infoClass(Object o) {
			System.out.println(o.getClass().getName());
		}
	}

	public static class Rect {
		public final int width;
		public final transient int height;

		@HideFromJS
		public Rect(int w, int h) {
			width = w;
			height = h;
		}

		public Rect(int w, int h, int d) {
			this(w, h);
			System.out.println("Depth: " + d);
		}
	}

	public static class EventsJS {
		public Consumer<Object> lastCallback;
		private final DynamicMap<DynamicMap<Integer>> dynamicMap0 = new DynamicMap<>(s1 -> new DynamicMap<>(s2 -> s1.hashCode() + s2.hashCode()));
		public Identifier someIdField = null;

		public void listen(String id, Consumer<Object> callback) {
			lastCallback = callback;
			System.out.println(id + ": " + callback);

			callback.accept(309);
		}

		public void testList(List<Object> strings) {
			System.out.println(strings.size());
		}

		public void testArray(int[] strings) {
			System.out.println(strings.length);
		}

		public void testMap(Map<String, Object> strings) {
			System.out.println(strings.size());
		}

		public String getAbc() {
			return "ABC";
		}

		public boolean isAbcd() {
			return true;
		}

		public void setAbc(String val) {
		}

		public static class DataTest {
			public int someInt;
			public String someString;
		}

		public void testData(DataObject data) {
			for (DataTest test : data.createDataObjectList(DataTest::new)) {
				System.out.println("Test: " + test.someString + " : " + test.someInt);
			}
		}

		public int[] getNumberList() {
			return new int[]{20, 94, 3034, -3030};
		}

		public int setNumberList(int[] i) {
			System.out.println("Set number list: " + Arrays.toString(i));
			return 0;
		}

		public DynamicMap<DynamicMap<Integer>> getDynamicMap() {
			return dynamicMap0;
		}

		@RemapForJS("testWrapper")
		public void testWrapper123(Identifier item, int a, int b, int c) {
			System.out.println("Testing wrapper: " + item + ", " + a + ", " + b + ", " + c);
		}

		@RemapForJS("testWrapper2")
		public void testWrapper123(Identifier[] item) {
			System.out.println("Testing wrapper: " + Arrays.asList(item));
		}

		@RemapForJS("testWrapper3")
		public void testWrapper123(Identifier[][][] item) {
			System.out.println("Testing wrapper: " + Arrays.asList(item));
		}

		public void setSomeId(String id) {
			System.out.println("Some ID set to (String): " + id);
		}

		public void setSomeId(Identifier id) {
			System.out.println("Some ID set to: " + id);
		}
	}
}
