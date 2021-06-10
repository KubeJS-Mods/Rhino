package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.SpecialEquality;

/**
 * @author LatvianModder
 */
public class ResourceLocation implements SpecialEquality {
	public final String namespace;
	public final String path;

	public ResourceLocation(String n, String p) {
		namespace = n;
		path = p;
	}

	public ResourceLocation(String i) {
		int c = i.indexOf(':');

		if (c == -1) {
			namespace = "minecraft";
			path = i;
		} else {
			namespace = i.substring(0, c);
			path = i.substring(c + 1);
		}
	}

	public ResourceLocation(Object anyObject) {
		this(anyObject.toString());
	}

	@Override
	public String toString() {
		return "ResourceLocation{" + namespace + ':' + path + '}';
	}

	@Override
	public boolean specialEquals(Object o, boolean shallow) {
		ResourceLocation r = o instanceof ResourceLocation ? (ResourceLocation) o : new ResourceLocation(String.valueOf(o));
		return r == this || namespace.equals(r.namespace) && path.equals(r.path);
	}
}
