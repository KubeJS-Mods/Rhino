package dev.latvian.mods.rhino.type;

import java.util.IdentityHashMap;
import java.util.Map;

public class InterfaceTypeInfo extends ClassTypeInfo {
	static final Map<Class<?>, InterfaceTypeInfo> CACHE = new IdentityHashMap<>();

	private Boolean functional;

	InterfaceTypeInfo(Class<?> type, Boolean functional) {
		super(type);
		this.functional = functional;
	}

	InterfaceTypeInfo(Class<?> type) {
		this(type, null);
	}

	@Override
	public boolean isFunctionalInterface() {
		if (functional == null) {
			functional = Boolean.FALSE;

			try {
				if (asClass().isAnnotationPresent(FunctionalInterface.class)) {
					functional = Boolean.TRUE;
				} else {
					int count = 0;

					for (var method : asClass().getMethods()) {
						if (!method.isDefault() && !method.isSynthetic() && !method.isBridge()) {
							count++;
						}

						if (count > 1) {
							break;
						}
					}

					if (count == 1) {
						functional = Boolean.TRUE;
					}
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}

		return functional;
	}
}
