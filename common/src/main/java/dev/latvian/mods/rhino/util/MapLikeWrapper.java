package dev.latvian.mods.rhino.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Set;

public class MapLikeWrapper<K, V> extends AbstractMap<K, V> {
	public final MapLike<K, V> mapLike;

	public MapLikeWrapper(MapLike<K, V> mapLike) {
		this.mapLike = mapLike;
	}

	@Override
	public V get(Object key) {
		return mapLike.getML(key);
	}

	@Override
	public boolean containsKey(Object key) {
		return mapLike.containsKeyML(key);
	}

	@Override
	public V put(K key, V value) {
		return mapLike.putML(key, value);
	}

	@NotNull
	@Override
	public Set<K> keySet() {
		return mapLike.keysML();
	}

	@NotNull
	@Override
	public Set<Entry<K, V>> entrySet() {
		return mapLike.entrySetML();
	}

	@Override
	public V remove(Object key) {
		return mapLike.removeML(key);
	}

	@Override
	public void clear() {
		mapLike.clearML();
	}
}
