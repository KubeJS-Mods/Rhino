package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

public class ContextFactory {
	private final ThreadLocal<Context> currentContext;
	private final TypeWrappers typeWrappers;
	private final Map<Class<?>, Object[]> defaultRecordProperties;
	private final MethodHandles.Lookup methodHandlesLookup;
	private final Map<Class<?>, MethodHandle> recordConstructors;
	private boolean instanceStaticFallback;

	public ContextFactory() {
		this.currentContext = ThreadLocal.withInitial(this::createContext);
		this.typeWrappers = new TypeWrappers();
		this.defaultRecordProperties = new IdentityHashMap<>();
		this.methodHandlesLookup = MethodHandles.publicLookup();
		this.recordConstructors = new IdentityHashMap<>();
		this.instanceStaticFallback = true;
	}

	protected Context createContext() {
		return new Context(this);
	}

	// private Context unsafeContext;

	public Context enter() {
		// if (unsafeContext == null) {
		//	unsafeContext = createContext();
		// }

		// return unsafeContext;
		return currentContext.get();
	}

	public synchronized TypeWrappers getTypeWrappers() {
		return typeWrappers;
	}

	public synchronized void registerDefaultRecordProperties(Record record) {
		try {
			var components = record.getClass().getRecordComponents();
			var properties = new Object[components.length];

			for (int i = 0; i < components.length; i++) {
				properties[i] = components[i].getAccessor().invoke(record);
			}

			defaultRecordProperties.put(record.getClass(), properties);
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Nullable
	public synchronized Object[] getDefaultRecordProperties(Class<?> type) {
		return defaultRecordProperties.get(type);
	}

	public MethodHandles.Lookup getMethodHandlesLookup() {
		return methodHandlesLookup;
	}

	@Nullable
	public synchronized MethodHandle getRecordConstructor(Class<?> type) {
		if (!type.isRecord()) {
			return null;
		}

		var constructor = recordConstructors.get(type);

		if (constructor == null) {
			try {
				var components = type.getRecordComponents();
				var args = new Class<?>[components.length];

				for (int i = 0; i < components.length; i++) {
					args[i] = components[i].getType();
				}

				constructor = getMethodHandlesLookup().findConstructor(type, MethodType.methodType(void.class, args));
			} catch (Exception ex) {
				return null;
			}

			recordConstructors.put(type, constructor);
		}

		return constructor;
	}

	public void setInstanceStaticFallback(boolean value) {
		instanceStaticFallback = value;
	}

	public boolean getInstanceStaticFallback() {
		return instanceStaticFallback;
	}

	public CachedClassStorage getCachedClassStorage() {
		return CachedClassStorage.GLOBAL_PUBLIC;
	}
}
