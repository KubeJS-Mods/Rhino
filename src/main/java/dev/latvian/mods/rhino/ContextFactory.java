package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProvider;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProviderHolder;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.Remapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ContextFactory {
	private final ThreadLocal<Context> currentContext;
	private TypeWrappers typeWrappers;
	private Remapper remapper;
	private final List<CustomJavaToJsWrapperProviderHolder<?>> customScriptableWrappers;
	private final Map<Class<?>, CustomJavaToJsWrapperProvider> customScriptableWrapperCache;
	private ClassShutter classShutter;

	public ContextFactory() {
		this.currentContext = ThreadLocal.withInitial(this::createContext);
		this.typeWrappers = new TypeWrappers();
		this.remapper = DefaultRemapper.INSTANCE;
		this.customScriptableWrappers = new ArrayList<>();
		this.customScriptableWrapperCache = new HashMap<>();
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
		if (typeWrappers == null) {
			typeWrappers = new TypeWrappers();
		}

		return typeWrappers;
	}

	public boolean hasTypeWrappers() {
		return typeWrappers != null;
	}

	public Remapper getRemapper() {
		return remapper;
	}

	public void setRemapper(Remapper remapper) {
		this.remapper = remapper;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public synchronized CustomJavaToJsWrapper wrapCustomJavaToJs(Object javaObject) {
		if (customScriptableWrappers.isEmpty()) {
			return null;
		}

		var provider = customScriptableWrapperCache.get(javaObject.getClass());

		if (provider == null) {
			for (CustomJavaToJsWrapperProviderHolder wrapper : customScriptableWrappers) {
				provider = wrapper.create(javaObject);

				if (provider != null) {
					break;
				}
			}

			if (provider == null) {
				provider = CustomJavaToJsWrapperProvider.NONE;
			}

			customScriptableWrapperCache.put(javaObject.getClass(), provider);
		}

		return provider.create(javaObject);
	}

	public <T> void addCustomJavaToJsWrapper(Predicate<T> predicate, CustomJavaToJsWrapperProvider<T> provider) {
		customScriptableWrappers.add(new CustomJavaToJsWrapperProviderHolder<>(predicate, provider));
	}

	public <T> void addCustomJavaToJsWrapper(Class<T> type, CustomJavaToJsWrapperProvider<T> provider) {
		addCustomJavaToJsWrapper(new CustomJavaToJsWrapperProviderHolder.PredicateFromClass<>(type), provider);
	}

	@Nullable
	public final synchronized ClassShutter getClassShutter() {
		return classShutter;
	}

	/**
	 * Set the LiveConnect access filter for this context.
	 * <p> {@link ClassShutter} may only be set if it is currently null.
	 * Otherwise a SecurityException is thrown.
	 *
	 * @param shutter a ClassShutter object
	 * @throws SecurityException if there is already a ClassShutter
	 *                           object for this Context
	 */
	public synchronized final void setClassShutter(ClassShutter shutter) {
		if (shutter == null) {
			throw new IllegalArgumentException();
		}

		if (classShutter != null) {
			throw new SecurityException("Cannot overwrite existing " + "ClassShutter object");
		}

		classShutter = shutter;
	}

	/**
	 * Create class loader for generated classes.
	 */
	public GeneratedClassLoader createClassLoader(ClassLoader parent) {
		return new DefiningClassLoader(parent);
	}
}
