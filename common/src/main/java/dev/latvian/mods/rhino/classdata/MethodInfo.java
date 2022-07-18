package dev.latvian.mods.rhino.classdata;

import java.lang.reflect.Method;

public class MethodInfo {
	public Method method;
	public String bean;
	public MethodSignature signature;
	public boolean isHidden;

	@Override
	public String toString() {
		return method.toString();
	}
}
