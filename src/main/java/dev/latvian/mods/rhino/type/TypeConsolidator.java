package dev.latvian.mods.rhino.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ZZZank
 */
public final class TypeConsolidator {
    private static final Map<Class<?>, Map<VariableTypeInfo, TypeInfo>> MAPPINGS = new IdentityHashMap<>();

    private static final boolean DEBUG = false;

    private TypeConsolidator() {
    }

    @NotNull
    public static Map<VariableTypeInfo, TypeInfo> getMapping(Class<?> type) {
        if (DEBUG) {
            System.out.println("getting mapping from: " + type);
        }
        var got = getImpl(type);
        return got == null ? Collections.emptyMap() : got;
    }

    @NotNull
    public static TypeInfo consolidateOrNone(VariableTypeInfo variable, Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(variable, TypeInfo.NONE);
    }

    @NotNull
    public static TypeInfo[] consolidateAll(
        @NotNull TypeInfo @NotNull [] original,
        @NotNull Map<VariableTypeInfo, TypeInfo> mapping
    ) {
        var len = original.length;
        if (DEBUG) {
            System.out.println("consolidating" + Arrays.toString(original));
        }
        if (len == 0) {
            return original;
        } else if (len == 1) {
            var consolidated = original[0].consolidate(mapping);
            return consolidated != original[0] ? new TypeInfo[]{consolidated} : original;
        }
        TypeInfo[] consolidatedAll = null;
        for (int i = 0; i < len; i++) {
            var type = original[i];
            var consolidated = type.consolidate(mapping);
            if (consolidated != type) {
                if (consolidatedAll == null) {
                    consolidatedAll = new TypeInfo[len];
                    System.arraycopy(original, 0, consolidatedAll, 0, i);
                }
                consolidatedAll[i] = consolidated;
            } else if (consolidatedAll != null) {
                consolidatedAll[i] = consolidated;
            }
        }
        return consolidatedAll == null ? original : consolidatedAll;
    }

	@NotNull
	public static List<@NotNull TypeInfo> consolidateAll(
		@NotNull List<@NotNull TypeInfo> original,
		@NotNull Map<VariableTypeInfo, TypeInfo> mapping
	) {
		var len = original.size();
		if (DEBUG) {
			System.out.println("consolidating" + original);
		}
		if (len == 0) {
			return original;
		} else if (len == 1) {
			var consolidated = original.getFirst().consolidate(mapping);
			return consolidated != original.getFirst() ? List.of(consolidated) : original;
		}
		List<@NotNull TypeInfo> consolidatedAll = null;
		for (int i = 0; i < len; i++) {
			var type = original.get(i);
			var consolidated = type.consolidate(mapping);
			if (consolidated != type) {
				if (consolidatedAll == null) {
					consolidatedAll = new ArrayList<>(len);
					consolidatedAll.addAll(original.subList(0, i));
				}
				consolidatedAll.set(i, consolidated);
			} else if (consolidatedAll != null) {
				consolidatedAll.set(i, consolidated);
			}
		}
		return consolidatedAll == null ? original : consolidatedAll;
	}

    @Nullable
    private static Map<VariableTypeInfo, TypeInfo> getImpl(Class<?> type) {
        if (type == null || type.isPrimitive() || type == Object.class) {
            return null;
        }
        synchronized (MAPPINGS) {
            return MAPPINGS.computeIfAbsent(type, TypeConsolidator::collect);
        }
    }

    @NotNull
    private static Map<VariableTypeInfo, TypeInfo> collect(Class<?> type) {
        var mapping = new IdentityHashMap<VariableTypeInfo, TypeInfo>();

        /**
         * (classes are named as 'XXX': A, B, C, ...)
         * (type variables are named as 'Tx': Ta, Tb, Tc, ...)
		 *
         * let's consider the most extreme case:
         * class A<Ta> {}
         * interface B<Tb> {}
         * class C<Tc> extends A<Tc> {}
         * class D<Td> extends C<Td> implements B<A<Td>> {}
         *
         * assuming that input 'type' is C.class
         */

        //collect current level mapping
        //current level types will only be consolidated by mappings from its subclasses
        var parent = type.getSuperclass();

        //in our D.class example, this will collect mapping from C<Td>, forming Tc -> Td
        extractSuperMapping(type.getGenericSuperclass(), mapping);

        //in our D.class example, this will collect mapping from B<A<Td>>, forming Tb -> A<Td>
        for (var genericInterface : type.getGenericInterfaces()) {
            extractSuperMapping(genericInterface, mapping);
        }

        //mapping from super
        //in our D.class example, super mapping will only include Ta -> Tc
        var superMapping = getImpl(parent);

        if (superMapping == null || superMapping.isEmpty()) {
            return postMapping(mapping);
        }

        //'flatten' super mapping
        var merged = new IdentityHashMap<>(superMapping);
        for (var entry : merged.entrySet()) {
            //in our D.class example, super mapping Ta -> Tc will be 'flattened' to Ta -> Td
            entry.setValue(entry.getValue().consolidate(mapping));
        }
        //merge two mapping
        merged.putAll(mapping);

        //in our D.class example, our mapping will include Ta -> Td, Tb -> A<Td>, Tc -> Td.
        //the 'flattened' means that all related type (Ta, Tb, Tc) can be directly mapped to
        //the type used by D.class (Td), so we only need to apply the mapping ONCE
        return postMapping(merged);
    }

    private static void extractSuperMapping(
        Type superType,
        IdentityHashMap<VariableTypeInfo, TypeInfo> pushTo
    ) {
        if (superType instanceof ParameterizedType parameterized
            && parameterized.getRawType() instanceof Class<?> parent
        ) {
            final var params = parent.getTypeParameters(); // T
            final var args = parameterized.getActualTypeArguments(); // T is mapped to
            for (int i = 0; i < args.length; i++) {
                pushTo.put(TypeInfo.of(params[i]), TypeInfo.of(args[i]));
            }
        }
    }

    private static Map<VariableTypeInfo, TypeInfo> postMapping(Map<VariableTypeInfo, TypeInfo> mapping) {
        switch (mapping.size()) {
            case 0:
                if (DEBUG) {
                    System.out.println("collected empty mapping");
                }
                return Collections.emptyMap();
            case 1:
                var entry = mapping.entrySet().iterator().next();
                if (DEBUG) {
                    System.out.println("collected singleton mapping: " + entry.getKey() + " -> " + entry.getValue());
                }
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            default:
                if (DEBUG) {
                    System.out.println("collected mapping with size: " + mapping.size());
                }
                return Collections.unmodifiableMap(mapping);
        }
    }
}
