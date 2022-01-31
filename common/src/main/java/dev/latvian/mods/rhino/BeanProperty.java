package dev.latvian.mods.rhino;

public class BeanProperty {
	BeanProperty(MemberBox getter, MemberBox setter, NativeJavaMethod setters) {
		this.getter = getter;
		this.setter = setter;
		this.setters = setters;
	}

	MemberBox getter;
	MemberBox setter;
	NativeJavaMethod setters;
}
