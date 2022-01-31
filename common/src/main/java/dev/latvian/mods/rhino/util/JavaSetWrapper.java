package dev.latvian.mods.rhino.util;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class JavaSetWrapper<T> extends AbstractList<T> {
	public final Set<T> set;

	public JavaSetWrapper(Set<T> set) {
		this.set = set;
	}

	@Override
	public T get(int index) {
		if (index < 0 || index >= size()) {
			throw new IndexOutOfBoundsException(index);
		} else if (index == 0) {
			return set.iterator().next();
		}

		for (T element : set) {
			if (index == 0) {
				return element;
			} else {
				index--;
			}
		}

		throw new IndexOutOfBoundsException(index);
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean add(T t) {
		return set.add(t);
	}

	@Override
	public void add(int index, T element) {
		set.add(element);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return set.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		return set.addAll(c);
	}

	@Override
	public T set(int index, T element) {
		if (set.remove(element)) {
			set.add(element);
			return null;
		} else {
			set.add(element);
			return element;
		}
	}

	@Override
	public T remove(int index) {
		if (index < 0 || index >= size()) {
			throw new IndexOutOfBoundsException(index);
		}

		Iterator<T> iterator = set.iterator();

		while (iterator.hasNext()) {
			T element = iterator.next();
			if (index == 0) {
				iterator.remove();
				return element;
			} else {
				index--;
			}
		}

		throw new IndexOutOfBoundsException(index);
	}

	@Override
	public boolean remove(Object o) {
		return set.remove(o);
	}

	@Override
	public void clear() {
		set.clear();
	}
}
