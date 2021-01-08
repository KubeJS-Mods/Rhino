package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.Context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add to a class or a parameter to indicate that it can be wrapped. Specify id if performance or precision is important
 *
 * @author LatvianModder
 * @see Context#getTypeWrappers()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Wrap
{
	String value() default "";

	/**
	 * Only affects interfaces
	 */
	boolean inherit() default false;
}
