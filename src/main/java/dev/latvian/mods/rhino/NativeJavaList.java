/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeJavaList extends NativeJavaObject {
	private static final TypeInfo REDUCE_FUNC_ARG = TypeInfo.of(BinaryOperator.class);

	public final List list;
	public final TypeInfo listType;

	public NativeJavaList(Context cx, Scriptable scope, Object jo, List list, TypeInfo type) {
		super(scope, jo, type, cx);
		this.list = list;
		this.listType = type.param(0);
	}

	@Override
	public String getClassName() {
		return "JavaList";
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return true;
		}
		return super.has(cx, key, start);
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			return cx.javaToJS(list.get(index), start, listType);
		}

		return Undefined.INSTANCE;
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return Boolean.TRUE;
		}
		return super.get(cx, key, start);
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (isWithValidIndex(index)) {
			list.set(index, cx.jsToJava(value, listType));
			return;
		}
		super.put(cx, index, start, value);
	}

	@Override
	public Object[] getIds(Context cx) {
		List<?> list = (List<?>) javaObject;
		Object[] result = new Object[list.size()];
		int i = list.size();
		while (--i >= 0) {
			result[i] = i;
		}
		return result;
	}

	private boolean isWithValidIndex(int index) {
		return index >= 0 && index < list.size();
	}

	@Override
	public void delete(Context cx, int index) {
		if (isWithValidIndex(index)) {
			Deletable.deleteObject(list.remove(index));
		}
	}

	@Override
	protected void initMembers(Context cx, Scriptable scope) {
		super.initMembers(cx, scope);
		var reduceFuncArg = REDUCE_FUNC_ARG.withParams(listType);

		addCustomProperty("length", TypeInfo.INT, this::getLength);
		addCustomFunction("push", TypeInfo.INT, this::push, TypeInfo.OBJECT);
		addCustomFunction("pop", listType, this::pop);
		addCustomFunction("shift", listType, this::shift);
		addCustomFunction("unshift", TypeInfo.INT, this::unshift, TypeInfo.OBJECT);
		addCustomFunction("concat", typeInfo, this::concat, TypeInfo.RAW_LIST);
		addCustomFunction("join", TypeInfo.STRING, this::join, TypeInfo.STRING);
		addCustomFunction("reverse", TypeInfo.NONE, this::reverse);
		addCustomFunction("slice", TypeInfo.NONE, this::slice, TypeInfo.OBJECT);
		addCustomFunction("splice", TypeInfo.NONE, this::splice, TypeInfo.OBJECT);
		addCustomFunction("every", TypeInfo.BOOLEAN, this::every, TypeInfo.RAW_PREDICATE);
		addCustomFunction("some", TypeInfo.BOOLEAN, this::some, TypeInfo.RAW_PREDICATE);
		addCustomFunction("filter", typeInfo, this::filter, TypeInfo.RAW_PREDICATE);
		addCustomFunction("map", TypeInfo.RAW_LIST, this::map, TypeInfo.RAW_FUNCTION);
		addCustomFunction("reduce", listType, this::reduce, reduceFuncArg);
		addCustomFunction("reduceRight", listType, this::reduceRight, reduceFuncArg);
		addCustomFunction("find", listType, this::find, TypeInfo.RAW_PREDICATE);
		addCustomFunction("findIndex", TypeInfo.NONE, this::findIndex, TypeInfo.RAW_PREDICATE);
		addCustomFunction("findLast", listType, this::findLast, TypeInfo.RAW_PREDICATE);
		addCustomFunction("findLastIndex", TypeInfo.NONE, this::findLastIndex, TypeInfo.RAW_PREDICATE);
	}

	private int getLength(Context cx) {
		return list.size();
	}

	private int push(Context cx, Object[] args) {
		if (args.length == 1) {
			list.add(cx.jsToJava(args[0], listType));
		} else if (args.length > 1) {
			Object[] args1 = new Object[args.length];

			for (int i = 0; i < args.length; i++) {
				args1[i] = cx.jsToJava(args[i], listType);
			}

			list.addAll(Arrays.asList(args1));
		}

		return list.size();
	}

	private Object pop(Context cx) {
		if (list.isEmpty()) {
			return Undefined.INSTANCE;
		}

		return list.removeLast();
	}

	private Object shift(Context cx) {
		if (list.isEmpty()) {
			return Undefined.INSTANCE;
		}

		return list.removeFirst();
	}

	private int unshift(Context cx, Object[] args) {
		for (int i = args.length - 1; i >= 0; i--) {
			list.addFirst(cx.jsToJava(args[i], listType));
		}

		return list.size();
	}

	private Object concat(Context cx, Object[] args) {
		List<Object> list1 = new ArrayList<>(list);

		if (args.length > 0 && args[0] instanceof List<?>) {
			list1.addAll((List<?>) cx.jsToJava(args[0], typeInfo));
		}

		return list1;
	}

	private String join(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return "";
		} else if (list.size() == 1) {
			return ScriptRuntime.toString(cx, list.getFirst());
		}

		String j = ScriptRuntime.toString(cx, args[0]);
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(j);
			}

			sb.append(ScriptRuntime.toString(cx, list.get(i)));
		}

		return sb.toString();
	}

	private NativeJavaList reverse(Context cx) {
		if (list.size() > 1) {
			Collections.reverse(list);
		}

		return this;
	}

	private Object slice(Context cx, Object[] args) {
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object splice(Context cx, Object[] args) {
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object every(Context cx, Object[] args) {
		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (!predicate.test(o)) {
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	private Object some(Context cx, Object[] args) {
		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (predicate.test(o)) {
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	private Object filter(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return this;
		}

		Predicate predicate = (Predicate) args[0];
		List<Object> list1 = new ArrayList<>();

		for (Object o : list) {
			if (predicate.test(o)) {
				list1.add(o);
			}
		}

		return list1;
	}

	private Object map(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return this;
		}

		Function function = (Function) args[0];

		List<Object> list1 = new ArrayList<>();

		for (Object o : list) {
			list1.add(function.apply(o));
		}

		return list1;
	}

	private Object reduce(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.INSTANCE;
		} else if (list.size() == 1) {
			return list.getFirst();
		}

		BinaryOperator operator = (BinaryOperator) args[0];
		Object o = get(cx, 0, this);

		for (int i = 1; i < list.size(); i++) {
			o = operator.apply(o, get(cx, i, this));
		}

		return o;
	}

	private Object reduceRight(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.INSTANCE;
		} else if (list.size() == 1) {
			return list.getFirst(); // might not be correct start index
		}

		BinaryOperator operator = (BinaryOperator) args[0];
		Object o = get(cx, 0, this);

		for (int i = list.size() - 1; i >= 1; i--) {
			o = operator.apply(o, get(cx, i, this));
		}

		return o;
	}

	private Object find(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.INSTANCE;
		}

		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (predicate.test(o)) {
				return o;
			}
		}

		return Undefined.INSTANCE;
	}

	private Object findIndex(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return -1;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = 0; i < list.size(); i++) {
			if (predicate.test(list.get(i))) {
				return i;
			}
		}

		return -1;
	}

	private Object findLast(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.INSTANCE;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = list.size() - 1; i >= 0; i--) {
			var o = list.get(i);

			if (predicate.test(o)) {
				return o;
			}
		}

		return Undefined.INSTANCE;
	}

	private Object findLastIndex(Context cx, Object[] args) {
		if (list.isEmpty()) {
			return -1;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = list.size() - 1; i >= 0; i--) {
			if (predicate.test(list.get(i))) {
				return i;
			}
		}

		return -1;
	}
}
