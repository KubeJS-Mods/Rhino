package dev.latvian.mods.rhino.mod.util;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface ChangeListener<T> {
	void onChanged(T o);
}