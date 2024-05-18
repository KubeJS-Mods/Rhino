package dev.latvian.mods.rhino.util.wrap;

/**
 * @author LatvianModder
 */
public record TypeWrapper<T>(Class<T> target, TypeWrapperValidator validator, TypeWrapperFactory<T> factory) {
}
