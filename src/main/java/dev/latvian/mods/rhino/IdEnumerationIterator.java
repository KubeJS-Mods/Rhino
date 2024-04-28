package dev.latvian.mods.rhino;

import java.util.function.Consumer;

public interface IdEnumerationIterator {
	boolean enumerationIteratorHasNext(Context cx, Consumer<Object> callback);

	boolean enumerationIteratorNext(Context cx, Consumer<Object> callback) throws JavaScriptException;
}
