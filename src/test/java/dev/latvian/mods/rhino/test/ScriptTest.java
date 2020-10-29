package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.util.DynamicFunction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class ScriptTest
{
	public static void main(String[] args)
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ScriptTest.class.getResourceAsStream("/rhino_test_script.js"), StandardCharsets.UTF_8)))
		{
			Context cx = Context.enter();

			cx.setClassShutter(fullClassName -> {
				System.out.println(fullClassName);
				return true;
			});

			Scriptable scope = cx.initStandardObjects();

			ScriptableObject.putProperty(scope, "console", Context.javaToJS(new ConsoleJS(), scope));

			EventsJS eventsJS = new EventsJS();

			ScriptableObject.putProperty(scope, "events", Context.javaToJS(eventsJS, scope));
			ScriptableObject.putProperty(scope, "sqTest", Context.javaToJS(new DynamicFunction(o -> ((Number) o[0]).doubleValue() * ((Number) o[0]).doubleValue()), scope));

			cx.evaluateReader(scope, reader, "rhino_test_script.js", 1, null);

			eventsJS.lastCallback.accept(48);
		}
		catch (RhinoException ex)
		{
			StringBuilder sb = new StringBuilder("Script error in ");
			sb.append(ex.sourceName());
			sb.append(':');
			sb.append(ex.lineNumber());

			if (ex.columnNumber() > 0)
			{
				sb.append(':');
				sb.append(ex.columnNumber());
			}

			sb.append(": ");
			sb.append(ex.details());
			System.err.println(sb.toString());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			Context.exit();
		}
	}

	public static class ConsoleJS
	{
		public void info(Object o)
		{
			System.out.println(o);
		}
	}

	public static class EventsJS
	{
		public Consumer<Object> lastCallback;

		public void listen(String id, Consumer<Object> callback)
		{
			lastCallback = callback;
			System.out.println(id + ": " + callback);

			callback.accept(309);
		}

		public void testList(List<Object> strings)
		{
			System.out.println(strings.size());
		}

		public void testArray(int[] strings)
		{
			System.out.println(strings.length);
		}

		public void testMap(Map<String, Object> strings)
		{
			System.out.println(strings.size());
		}

		public String getAbc()
		{
			return "ABC";
		}

		public boolean isAbcd()
		{
			return true;
		}

		public void setAbc(String val)
		{
		}
	}
}
