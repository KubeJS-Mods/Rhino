/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

class SpecialRef extends Ref {
	private static final int SPECIAL_NONE = 0;
	private static final int SPECIAL_PROTO = 1;
	private static final int SPECIAL_PARENT = 2;

	static Ref createSpecial(Context cx, Scriptable scope, Object object, String name) {
		Scriptable target = ScriptRuntime.toObjectOrNull(cx, object, scope);
		if (target == null) {
			throw ScriptRuntime.undefReadError(cx, object, name);
		}

		int type;
		if (name.equals("__proto__")) {
			type = SPECIAL_PROTO;
		} else if (name.equals("__parent__")) {
			type = SPECIAL_PARENT;
		} else {
			throw new IllegalArgumentException(name);
		}

		return new SpecialRef(target, type, name);
	}

	private final Scriptable target;
	private final int type;
	private final String name;

	private SpecialRef(Scriptable target, int type, String name) {
		this.target = target;
		this.type = type;
		this.name = name;
	}

	@Override
	public Object get(Context cx) {
		return switch (type) {
			case SPECIAL_NONE -> ScriptRuntime.getObjectProp(cx, target, name);
			case SPECIAL_PROTO -> target.getPrototype(cx);
			case SPECIAL_PARENT -> target.getParentScope();
			default -> throw Kit.codeBug();
		};
	}

	@Override
	@Deprecated
	public Object set(Context cx, Object value) {
		throw new IllegalStateException();
	}

	@Override
	public Object set(Context cx, Scriptable scope, Object value) {
		switch (type) {
			case SPECIAL_NONE:
				return ScriptRuntime.setObjectProp(cx, target, name, value);
			case SPECIAL_PROTO:
			case SPECIAL_PARENT: {
				Scriptable obj = ScriptRuntime.toObjectOrNull(cx, value, scope);
				if (obj != null) {
					// Check that obj does not contain on its prototype/scope
					// chain to prevent cycles
					Scriptable search = obj;
					do {
						if (search == target) {
							throw Context.reportRuntimeError1("msg.cyclic.value", name, cx);
						}
						if (type == SPECIAL_PROTO) {
							search = search.getPrototype(cx);
						} else {
							search = search.getParentScope();
						}
					} while (search != null);
				}
				if (type == SPECIAL_PROTO) {
					if (target instanceof ScriptableObject && !((ScriptableObject) target).isExtensible()) {
						throw ScriptRuntime.typeError0(cx, "msg.not.extensible");
					}

					if ((value != null && ScriptRuntime.typeof(cx, value) != MemberType.OBJECT) || ScriptRuntime.typeof(cx, target) != MemberType.OBJECT) {
						return Undefined.instance;
					}
					target.setPrototype(obj);
				} else {
					target.setParentScope(obj);
				}
				return obj;
			}
			default:
				throw Kit.codeBug();
		}
	}

	@Override
	public boolean has(Context cx) {
		if (type == SPECIAL_NONE) {
			return ScriptRuntime.hasObjectElem(cx, target, name);
		}
		return true;
	}

	@Override
	public boolean delete(Context cx) {
		if (type == SPECIAL_NONE) {
			return ScriptRuntime.deleteObjectElem(cx, target, name);
		}
		return false;
	}
}

