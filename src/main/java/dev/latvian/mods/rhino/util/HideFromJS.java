package dev.latvian.mods.rhino.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to hide objects in java classes from javascript.
 * If added to a member (field or method), they will act as undefined / non-existant.
 * If added to a class, all members will be hidden.
 * If added to a constructor, new Type() will be hidden.
 * If added to a package, all classes, members and constructors will be hidden.
 * For fields <code>transient</code> keyword can be used instead of this annotation.
 *
 * @author LatvianModder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface HideFromJS
{
}
