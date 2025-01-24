package dev.latvian.mods.rhino.type;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author ZZZank
 * @author Prunoideae
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
	 * Variable name is needed for type dumping purpose, otherwise I still need
	 * to create a resolver completely parallel to {@link dev.latvian.mods.rhino.type.TypeInfo#of(Class)}
	 */
	private final String name;

	public VariableTypeInfo(String name) {
		this.name = name;
	}

	static TypeInfo of(TypeVariable<?> t) {
		READ.lock();
		var got = CACHE.get(t);
		READ.unlock();
		if (got == null) {
			WRITE.lock();
			// a variable type can have multiple bounds, but we only resolves the first one, since type wrapper cannot
			// magically find or create a class that meets multiple bounds
			Type bound = t.getBounds()[0];
			VariableTypeInfo variable = new VariableTypeInfo(t.getName());
			CACHE.put(t, got = variable);
			variable.consolidated = TypeInfo.of(bound);
			WRITE.unlock();
		}
		return got;
	}

	@Override
	public Class<?> asClass() {
		return consolidated.asClass();
	}

	public String getName() {
		return name;
	}
}
