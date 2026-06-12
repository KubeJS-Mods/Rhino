package dev.latvian.mods.rhino;

/**
 * Abstract Object Operations as defined by EcmaScript
 *
 * @see <a href="https://262.ecma-international.org/11.0/#sec-operations-on-objects">Abstract Operations - Operations on Objects</a>
 */
class AbstractEcmaObjectOperations {
	enum INTEGRITY_LEVEL {
		FROZEN,
		SEALED
	}

	/**
	 * Implementation of Abstract Object operation HasOwnProperty as defined by EcmaScript
	 *
	 * @see <a href="https://262.ecma-international.org/12.0/#sec-hasownproperty">HasOwnProperty</a>
	 */
	static boolean hasOwnProperty(Context cx, Object o, Object property) {
		ScriptableObject obj = ScriptableObject.ensureScriptableObject(o, cx);
		boolean result;
		if (property instanceof Symbol sym) {
			result = ScriptableObject.ensureSymbolScriptable(o, cx).has(cx, sym, obj);
		} else {
			ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, property);
			if (s.stringId == null) {
				result = obj.has(cx, s.index, obj);
			} else {
				result = obj.has(cx, s.stringId, obj);
			}
		}

		return result;
	}

	/**
	 * Implementation of Abstract Object operation testIntegrityLevel as defined by EcmaScript
	 *
	 * @see <a href="https://262.ecma-international.org/11.0/#sec-testintegritylevel">TestIntegrityLevel</a>
	 */
	static boolean testIntegrityLevel(Context cx, Object o, INTEGRITY_LEVEL level) {
		ScriptableObject obj = ScriptableObject.ensureScriptableObject(o, cx);

		if (obj.isExtensible()) {
			return Boolean.FALSE;
		}

		for (Object name : obj.getIds(cx, true, true)) {
			ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
			if (Boolean.TRUE.equals(desc.get(cx, "configurable"))) {
				return Boolean.FALSE;
			}

			if (level == INTEGRITY_LEVEL.FROZEN && obj.isDataDescriptor(desc, cx) && Boolean.TRUE.equals(desc.get(cx, "writable"))) {
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	/**
	 * Implementation of Abstract Object operation setIntegrityLevel as defined by EcmaScript
	 * <p>
	 * Implementation currently tightly coupled with ScriptableObject impl.
	 * which allow some optimization.
	 *
	 * @see <a href="https://262.ecma-international.org/11.0/#sec-setintegritylevel">SetIntegrityLevel</a>
	 */
	static boolean setIntegrityLevel(Context cx, Object o, INTEGRITY_LEVEL level) {
		/*
			1. Assert: Type(O) is Object.
			2. Assert: level is either sealed or frozen.
			3. Let status be ? O.[[PreventExtensions]]().
			4. If status is false, return false.
			5. Let keys be ? O.[[OwnPropertyKeys]]().
			6. If level is sealed, then
				a. For each element k of keys, do
					i. Perform ? DefinePropertyOrThrow(O, k, PropertyDescriptor { [[Configurable]]: false }).
			7. Else,
				a. Assert: level is frozen.
				b. For each element k of keys, do
					i. Let currentDesc be ? O.[[GetOwnProperty]](k).
					ii. If currentDesc is not undefined, then
						1. If IsAccessorDescriptor(currentDesc) is true, then
							a. Let desc be the PropertyDescriptor { [[Configurable]]: false }.
						2. Else,
							a. Let desc be the PropertyDescriptor { [[Configurable]]: false, [[Writable]]: false }.
						3. Perform ? DefinePropertyOrThrow(O, k, desc).
			8. Return true.

			NOTES
			- Step 3 calls for the .preventExtensions() before updating the propertyDescriptors,
			  In Rhino however .preventExtensions() never returns false
			  and calling it before will block updating the propertyDescriptors afterwards
			- While steps 6.a.i and 7.b.ii.3 call for the Abstract DefinePropertyOrThrow operation,
			  the conditions under which a throw would occur aren't applicable when freezing or sealing an object
		 */
		ScriptableObject obj = ScriptableObject.ensureScriptableObject(o, cx);

		for (Object key : obj.getIds(cx, true, true)) {
			ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, key);

			if (level == INTEGRITY_LEVEL.SEALED) {
				if (Boolean.TRUE.equals(desc.get(cx, "configurable"))) {
					desc.put(cx, "configurable", desc, Boolean.FALSE);

					obj.defineOwnProperty(cx, key, desc, false);
				}
			} else {
				if (obj.isDataDescriptor(desc, cx) && Boolean.TRUE.equals(desc.get(cx, "writable"))) {
					desc.put(cx, "writable", desc, Boolean.FALSE);
				}
				if (Boolean.TRUE.equals(desc.get(cx, "configurable"))) {
					desc.put(cx, "configurable", desc, Boolean.FALSE);
				}
				obj.defineOwnProperty(cx, key, desc, false);
			}
		}

		obj.preventExtensions();

		return true;
	}
}
