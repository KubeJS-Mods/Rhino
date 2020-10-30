package dev.latvian.mods.rhino.util;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public interface DataObject
{
	<T> T createDataObject(Supplier<T> instanceFactory);

	<T> List<T> createDataObjectList(Supplier<T> instanceFactory);

	boolean isDataObjectList();
}
