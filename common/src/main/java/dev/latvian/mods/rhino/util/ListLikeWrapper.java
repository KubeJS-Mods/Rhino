package dev.latvian.mods.rhino.util;

import java.util.AbstractList;

public class ListLikeWrapper<T> extends AbstractList<T> {
	public final ListLike<T> listLike;

	public ListLikeWrapper(ListLike<T> listLike) {
		this.listLike = listLike;
	}

	@Override
	public T get(int index) {
		return listLike.getLL(index);
	}

	@Override
	public int size() {
		return listLike.sizeLL();
	}

	@Override
	public boolean add(T t) {
		return listLike.addLL(t);
	}

	@Override
	public void add(int index, T element) {
		listLike.addLL(index, element);
	}

	@Override
	public T set(int index, T element) {
		return listLike.setLL(index, element);
	}

	@Override
	public T remove(int index) {
		return listLike.removeLL(index);
	}

	@Override
	public void clear() {
		listLike.clearLL();
	}
}
