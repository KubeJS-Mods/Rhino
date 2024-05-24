package dev.latvian.mods.rhino.type;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RecordTypeInfo extends ClassTypeInfo {
	public record Component(String name, TypeInfo type) {
	}

	static final Map<Class<?>, RecordTypeInfo> CACHE = new IdentityHashMap<>();

	private Map<String, Component> recordComponents;

	RecordTypeInfo(Class<?> type) {
		super(type);
	}

	@Override
	public Map<String, Component> recordComponents() {
		if (recordComponents == null) {
			recordComponents = new LinkedHashMap<>();

			for (var field : asClass().getRecordComponents()) {
				recordComponents.put(field.getName(), new Component(field.getName(), TypeInfo.of(field.getGenericType())));
			}
		}

		return recordComponents;
	}
}
