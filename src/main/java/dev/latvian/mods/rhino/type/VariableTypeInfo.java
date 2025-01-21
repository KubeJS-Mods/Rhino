package dev.latvian.mods.rhino.type;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author ZZZank
 */
public class VariableTypeInfo extends TypeInfoBase {
	private static final Map<TypeVariable<?>, TypeInfo> CACHE = new HashMap<>();
	private static final Lock READ;
	private static final Lock WRITE;

	static {
		ReentrantReadWriteLock l = new ReentrantReadWriteLock();
		READ = l.readLock();
		WRITE = l.writeLock();
	}

	private TypeInfo consolidated = null;

	/**
	 * we don't need type name to match a TypeVariable, it's designed to be unique
	 */
//    private final String name;
	static TypeInfo of(TypeVariable<?> t) {
		READ.lock();
		var got = CACHE.get(t);
		READ.unlock();
		if (got == null) {
			WRITE.lock();
			// a variable type can have multiple bounds, but we only resolves the first one, since type wrapper cannot
			// magically find or create a class that meets multiple bounds
			Type bound = t.getBounds()[0];
			if (bound == Object.class) {
				CACHE.put(t, got = TypeInfo.NONE);
			} else {
				VariableTypeInfo variable = new VariableTypeInfo();
				CACHE.put(t, got = variable);
				/*
				 * we cannot create VariableTypeInfo directly, because types like {@code Enum<T extends Enum<T>>} will
				 * cause TypeInfo.of() to parse the same variable type `T` infinitely, instead we push it into cache
				 * before next TypeInfo.of(...) call to make sure that `T` in `Enum<T>` in `T extends Enum<T>` is
				 * already parsed, so that we can break the loop
				 */
				variable.consolidated = TypeInfo.of(bound);
			}
			WRITE.unlock();
		}
		return got;
	}

	@Override
	public Class<?> asClass() {
		return consolidated.asClass();
	}
}
