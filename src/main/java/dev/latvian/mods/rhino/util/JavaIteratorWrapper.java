package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.IdEnumerationIterator;
import dev.latvian.mods.rhino.JavaScriptException;

import java.util.Iterator;
import java.util.function.Consumer;

public record JavaIteratorWrapper(Iterator<?> parent) implements IdEnumerationIterator {
	@Override
	public boolean enumerationIteratorHasNext(Context cx, Consumer<Object> callback) {
		if (parent.hasNext()) {
			callback.accept(parent.next());
			return true;
		}

		return false;
	}

	@Override
	public boolean enumerationIteratorNext(Context cx, Consumer<Object> callback) throws JavaScriptException {
		if (parent.hasNext()) {
			callback.accept(parent.next());
			return true;
		}

		return false;
	}
}
