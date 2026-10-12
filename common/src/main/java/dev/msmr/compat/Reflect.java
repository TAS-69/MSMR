package dev.msmr.compat;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Optional APIs stay outside the required class path. Only public methods are used. */
public final class Reflect {
    private record Key(String name, boolean statik, List<Class<?>> arguments) {}
    private static final ClassValue<Map<Key, Method>> methods = new ClassValue<>() {
        protected Map<Key, Method> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };
    private Reflect() {}
    public static Class<?> optional(String name) {
        try { return Class.forName(name, false, Reflect.class.getClassLoader()); }
        catch (ClassNotFoundException ex) { return null; }
    }
    public static Class<?> type(Object plugin, String name) throws ClassNotFoundException {
        return Class.forName(name, false, plugin.getClass().getClassLoader());
    }
    public static Object call(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        boolean statik = receiver instanceof Class<?>;
        Class<?> type = statik ? (Class<?>) receiver : receiver.getClass();
        Key key = new Key(name, statik, Arrays.stream(args).map(a -> a == null ? Void.class : a.getClass()).toList());
        Method method = methods.get(type).get(key);
        if (method == null) {
            List<Method> matches = Arrays.stream(type.getMethods()).filter(m -> m.getName().equals(name)
                && Modifier.isStatic(m.getModifiers()) == statik && m.getParameterCount() == args.length && !m.isBridge()
                && compatible(m.getParameterTypes(), args)).toList();
            List<Method> candidates = matches.stream().filter(m -> matches.stream().noneMatch(other -> other != m && moreSpecific(other, m))).toList();
            if (candidates.size() != 1) throw new NoSuchMethodException(type.getName() + "." + name + " (" + candidates.size() + " matching methods)");
            method = candidates.getFirst();
            methods.get(type).put(key, method);
        }
        return method.invoke(statik ? null : receiver, args);
    }
    private static boolean moreSpecific(Method a, Method b) {
        boolean strict = false;
        for (int i = 0; i < a.getParameterCount(); i++) {
            if (!b.getParameterTypes()[i].isAssignableFrom(a.getParameterTypes()[i])) return false;
            strict |= a.getParameterTypes()[i] != b.getParameterTypes()[i];
        }
        return strict;
    }
    private static boolean compatible(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type.isPrimitive()) type = switch (type.getName()) {
                case "boolean" -> Boolean.class; case "int" -> Integer.class; case "long" -> Long.class;
                case "double" -> Double.class; case "float" -> Float.class; default -> type;
            };
            if (args[i] == null ? type.isPrimitive() : !type.isInstance(args[i])) return false;
        }
        return true;
    }
    public static boolean bool(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        return (Boolean) call(receiver, name, args);
    }
}
