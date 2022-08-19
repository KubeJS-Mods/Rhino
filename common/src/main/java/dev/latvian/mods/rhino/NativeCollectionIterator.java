package dev.latvian.mods.rhino;

import java.util.Collections;
import java.util.Iterator;

public class NativeCollectionIterator extends ES6Iterator {
	private final String className;
	private final Type type;
	private transient Iterator<Hashtable.Entry> iterator = Collections.emptyIterator();

	enum Type {
		KEYS, VALUES, BOTH
	}

	static void init(ScriptableObject scope, String tag, boolean sealed) {
		init(scope, sealed, new NativeCollectionIterator(tag), tag);
	}

	public NativeCollectionIterator(String tag) {
		this.className = tag;
		this.iterator = Collections.emptyIterator();
		this.type = Type.BOTH;
	}

	public NativeCollectionIterator(Scriptable scope, String className, Type type, Iterator<Hashtable.Entry> iterator) {
		super(scope, className);
		this.className = className;
		this.iterator = iterator;
		this.type = type;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	protected boolean isDone(Context cx, Scriptable scope) {
		return !iterator.hasNext();
	}

	@Override
	protected Object nextValue(Context cx, Scriptable scope) {
		final Hashtable.Entry e = iterator.next();
		return switch (type) {
			case KEYS -> e.key;
			case VALUES -> e.value;
			case BOTH -> cx.newArray(scope, new Object[]{e.key, e.value});
		};
	}
}
