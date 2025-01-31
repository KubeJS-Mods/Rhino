package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class CachedFieldInfo extends CachedMemberInfo {
	public static class Accessible {
		CachedFieldInfo info;
		String name = "";
		boolean hidden = false;

		public CachedFieldInfo getInfo() {
			return info;
		}

		public String getName() {
			return name;
		}

		boolean isHidden() {
			return hidden;
		}
	}

	final Field field;
	private TypeInfo type;
	private MethodHandle getterMethodHandle;
	private MethodHandle setterMethodHandle;

	public CachedFieldInfo(CachedClassInfo parent, Field f) {
		super(parent, f, f.getName(), f.getModifiers());
		this.field = f;
	}

	@Override
	public Field getCached() {
		return field;
	}

	public TypeInfo getType() {
		if (type == null) {
			type = TypeInfo.safeOf(field::getGenericType);
		}

		return type;
	}

	public Object get(Context cx, @Nullable Object instance) throws Throwable {
		var mh = getterMethodHandle;

		if (mh == null) {
			if (parent.storage.includeProtected && Modifier.isProtected(modifiers) && !field.isAccessible()) {
				field.setAccessible(true);
			}

			mh = getterMethodHandle = cx.factory.getMethodHandlesLookup().unreflectGetter(field);
		}

		if (isStatic) {
			return mh.invokeWithArguments();
		} else {
			return mh.invokeWithArguments(instance);
		}
	}

	public void set(Context cx, @Nullable Object instance, Object value) throws Throwable {
		var mh = setterMethodHandle;

		if (mh == null) {
			if (parent.storage.includeProtected && Modifier.isProtected(modifiers) && !field.isAccessible()) {
				field.setAccessible(true);
			}

			mh = setterMethodHandle = cx.factory.getMethodHandlesLookup().unreflectSetter(field);
		}

		if (isStatic) {
			mh.invokeWithArguments(value);
		} else {
			mh.invokeWithArguments(instance, value);
		}
	}
}
