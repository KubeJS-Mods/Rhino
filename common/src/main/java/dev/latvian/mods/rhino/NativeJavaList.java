/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.ValueUnwrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeJavaList extends NativeJavaObject {
	private final List list;
	private final Class<?> listType;
	private final ValueUnwrapper valueUnwrapper;

	public NativeJavaList(Scriptable scope, Object jo, List list, @Nullable Class<?> listType, ValueUnwrapper valueUnwrapper) {
		super(scope, jo, jo.getClass());
		this.list = list;
		this.listType = listType;
		this.valueUnwrapper = valueUnwrapper;
	}

	public NativeJavaList(Scriptable scope, Object jo, List list) {
		this(scope, jo, list, null, ValueUnwrapper.DEFAULT);
	}

	@Override
	public String getClassName() {
		return "JavaList";
	}

	@Override
	public boolean has(int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			return true;
		}
		return super.has(index, start);
	}

	@Override
	public boolean has(Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return true;
		}
		return super.has(key, start);
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			return valueUnwrapper.unwrap(this, list.get(index));
		}
		return Undefined.instance;
	}

	@Override
	public Object get(Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return Boolean.TRUE;
		}
		return super.get(key, start);
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		if (isWithValidIndex(index)) {
			list.set(index, Context.jsToJava(value, listType));
			return;
		}
		super.put(index, start, value);
	}

	@Override
	public Object[] getIds() {
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
	public void delete(int index) {
		if (isWithValidIndex(index)) {
			Deletable.deleteObject(list.remove(index));
		}
	}

	@Override
	protected void initMembers() {
		super.initMembers();
		addCustomProperty("length", this::getLength);
		addCustomFunction("push", this::push, Object.class);
		addCustomFunction("pop", this::pop);
		addCustomFunction("shift", this::shift);
		addCustomFunction("unshift", this::unshift, Object.class);
		addCustomFunction("concat", this::concat, List.class);
		addCustomFunction("join", this::join, String.class);
		addCustomFunction("reverse", this::reverse);
		addCustomFunction("slice", this::slice, Object.class);
		addCustomFunction("splice", this::splice, Object.class);
		addCustomFunction("every", this::every, Predicate.class);
		addCustomFunction("some", this::some, Predicate.class);
		addCustomFunction("filter", this::filter, Predicate.class);
		addCustomFunction("map", this::map, Function.class);
		addCustomFunction("reduce", this::reduce, BinaryOperator.class);
		addCustomFunction("reduceRight", this::reduceRight, BinaryOperator.class);
		addCustomFunction("find", this::find, Predicate.class);
		addCustomFunction("findIndex", this::findIndex, Predicate.class);
		addCustomFunction("findLast", this::findLast, Predicate.class);
		addCustomFunction("findLastIndex", this::findLastIndex, Predicate.class);
	}

	private int getLength() {
		return list.size();
	}

	private int push(Object[] args) {
		if (args.length == 1) {
			list.add(Context.jsToJava(args[0], listType));
		} else if (args.length > 1) {
			Object[] args1 = new Object[args.length];

			for (int i = 0; i < args.length; i++) {
				args1[i] = Context.jsToJava(args[i], listType);
			}

			list.addAll(Arrays.asList(args1));
		}

		return list.size();
	}

	private Object pop() {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		return list.remove(list.size() - 1);
	}

	private Object shift() {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		return list.remove(0);
	}

	private int unshift(Object[] args) {
		for (int i = args.length - 1; i >= 0; i--) {
			list.add(0, Context.jsToJava(args[i], listType));
		}

		return list.size();
	}

	private Object concat(Object[] args) {
		List<Object> list1 = new ArrayList<>(list);

		if (args.length > 0 && args[0] instanceof List<?>) {
			list1.addAll((List<?>) args[0]);
		}

		return list1;
	}

	private String join(Object[] args) {
		if (list.isEmpty()) {
			return "";
		} else if (list.size() == 1) {
			return ScriptRuntime.toString(list.get(0));
		}

		String j = ScriptRuntime.toString(args[0]);
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(j);
			}

			sb.append(ScriptRuntime.toString(list.get(i)));
		}

		return sb.toString();
	}

	private NativeJavaList reverse() {
		if (list.size() > 1) {
			Collections.reverse(list);
		}

		return this;
	}

	private Object slice(Object[] args) {
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object splice(Object[] args) {
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object every(Object[] args) {
		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (!predicate.test(o)) {
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	private Object some(Object[] args) {
		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (predicate.test(o)) {
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	private Object filter(Object[] args) {
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

	private Object map(Object[] args) {
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

	private Object reduce(Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		} else if (list.size() == 1) {
			return list.get(0);
		}

		BinaryOperator operator = (BinaryOperator) args[0];
		Object o = valueUnwrapper.unwrap(this, list.get(0));

		for (int i = 1; i < list.size(); i++) {
			o = valueUnwrapper.unwrap(this, operator.apply(o, valueUnwrapper.unwrap(this, list.get(i))));
		}

		return o;
	}

	private Object reduceRight(Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		} else if (list.size() == 1) {
			return list.get(0);
		}

		BinaryOperator operator = (BinaryOperator) args[0];
		Object o = valueUnwrapper.unwrap(this, list.get(0));

		for (int i = list.size() - 1; i >= 1; i--) {
			o = valueUnwrapper.unwrap(this, operator.apply(o, valueUnwrapper.unwrap(this, list.get(i))));
		}

		return o;
	}

	private Object find(Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (predicate.test(o)) {
				return o;
			}
		}

		return Undefined.instance;
	}

	private Object findIndex(Object[] args) {
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

	private Object findLast(Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = list.size() - 1; i >= 0; i--) {
			var o = list.get(i);

			if (predicate.test(o)) {
				return o;
			}
		}

		return Undefined.instance;
	}

	private Object findLastIndex(Object[] args) {
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
