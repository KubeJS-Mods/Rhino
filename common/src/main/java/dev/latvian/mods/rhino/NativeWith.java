/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the object lookup required for the
 * <code>with</code> statement.
 * It simply delegates every action to its prototype except
 * for operations on its parent.
 */
public class NativeWith implements Scriptable, SymbolScriptable, IdFunctionCall {
	private static final Object FTAG = "With";
	private static final int Id_constructor = 1;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeWith obj = new NativeWith();

		obj.setParentScope(scope);
		obj.setPrototype(ScriptableObject.getObjectPrototype(scope, cx));

		IdFunctionObject ctor = new IdFunctionObject(obj, FTAG, Id_constructor, "With", 0, scope);
		ctor.markAsConstructor(obj);
		if (sealed) {
			ctor.sealObject(cx);
		}
		ctor.exportAsScopeProperty(cx);
	}

	static boolean isWithFunction(Object functionObj) {
		if (functionObj instanceof IdFunctionObject f) {
			return f.hasTag(FTAG) && f.methodId() == Id_constructor;
		}
		return false;
	}

	static Object newWithSpecial(Context cx, Scriptable scope, Object[] args) {
		ScriptRuntime.checkDeprecated(cx, "With");
		scope = ScriptableObject.getTopLevelScope(scope);
		NativeWith thisObj = new NativeWith();
		thisObj.setPrototype(args.length == 0 ? ScriptableObject.getObjectPrototype(scope, cx) : ScriptRuntime.toObject(cx, scope, args[0]));
		thisObj.setParentScope(scope);
		return thisObj;
	}

	protected Scriptable prototype;
	protected Scriptable parent;

	private NativeWith() {
	}

	protected NativeWith(Scriptable parent, Scriptable prototype) {
		this.parent = parent;
		this.prototype = prototype;
	}

	@Override
	public String getClassName() {
		return "With";
	}

	@Override
	public boolean has(Context cx, String id, Scriptable start) {
		return prototype.has(cx, id, prototype);
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		if (prototype instanceof SymbolScriptable) {
			return ((SymbolScriptable) prototype).has(cx, key, prototype);
		}
		return false;
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		return prototype.has(cx, index, prototype);
	}

	@Override
	public Object get(Context cx, String id, Scriptable start) {
		if (start == this) {
			start = prototype;
		}
		return prototype.get(cx, id, start);
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (start == this) {
			start = prototype;
		}
		if (prototype instanceof SymbolScriptable) {
			return ((SymbolScriptable) prototype).get(cx, key, start);
		}
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (start == this) {
			start = prototype;
		}
		return prototype.get(cx, index, start);
	}

	@Override
	public void put(Context cx, String id, Scriptable start, Object value) {
		if (start == this) {
			start = prototype;
		}
		prototype.put(cx, id, start, value);
	}

	@Override
	public void put(Context cx, Symbol symbol, Scriptable start, Object value) {
		if (start == this) {
			start = prototype;
		}
		if (prototype instanceof SymbolScriptable) {
			((SymbolScriptable) prototype).put(cx, symbol, start, value);
		}
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (start == this) {
			start = prototype;
		}
		prototype.put(cx, index, start, value);
	}

	@Override
	public void delete(Context cx, String id) {
		prototype.delete(cx, id);
	}

	@Override
	public void delete(Context cx, Symbol key) {
		if (prototype instanceof SymbolScriptable) {
			((SymbolScriptable) prototype).delete(cx, key);
		}
	}

	@Override
	public void delete(Context cx, int index) {
		prototype.delete(cx, index);
	}

	@Override
	public Scriptable getPrototype(Context cx) {
		return prototype;
	}

	@Override
	public void setPrototype(Scriptable prototype) {
		this.prototype = prototype;
	}

	@Override
	public Scriptable getParentScope() {
		return parent;
	}

	@Override
	public void setParentScope(Scriptable parent) {
		this.parent = parent;
	}

	@Override
	public Object[] getIds(Context cx) {
		return prototype.getIds(cx);
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> typeHint) {
		return prototype.getDefaultValue(cx, typeHint);
	}

	@Override
	public boolean hasInstance(Context cx, Scriptable value) {
		return prototype.hasInstance(cx, value);
	}

	/**
	 * Must return null to continue looping or the final collection result.
	 */
	protected Object updateDotQuery(boolean value) {
		// NativeWith itself does not support it
		throw new IllegalStateException();
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (f.hasTag(FTAG)) {
			if (f.methodId() == Id_constructor) {
				throw Context.reportRuntimeError1("msg.cant.call.indirect", "With", cx);
			}
		}
		throw f.unknown();
	}
}
