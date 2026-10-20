package bottled.minetuner.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Test helper: fills every non-static field of an object graph with a non-default value and
 * compares two graphs field-by-field, so a copier that forgets a field is caught.
 */
final class ReflectionFill {

    private ReflectionFill() {
    }

    static void fill(Object target, Set<String> skip) throws Exception {
        for (Field f : fields(target.getClass())) {
            if (skip.contains(f.getName())) continue;
            f.setAccessible(true);
            f.set(target, value(f.getGenericType(), f.getName(), f.get(target)));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object value(Type type, String name, Object current) throws Exception {
        Class<?> raw = type instanceof ParameterizedType p ? (Class<?>) p.getRawType() : (Class<?>) type;
        if (raw == int.class || raw == Integer.class) return 7 + name.length();
        if (raw == long.class) return 11L + name.length();
        if (raw == float.class) return 0.25f + name.length() / 100f;
        if (raw == double.class) return 0.125 + name.length() / 1000.0;
        if (raw == boolean.class) return current instanceof Boolean b ? !b : true;
        if (raw == String.class) return "x-" + name;
        if (raw.isEnum()) {
            Object[] all = raw.getEnumConstants();
            for (Object c : all) if (!c.equals(current)) return c;
            return all[0];
        }
        if (Map.class.isAssignableFrom(raw)) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            Map m = new LinkedHashMap();
            m.put("MEMORY", value(args[1], name + "v", null));
            m.put("PING", value(args[1], name + "w", null));
            return m;
        }
        if (List.class.isAssignableFrom(raw)) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            List l = new ArrayList();
            l.add(value(args[0], name + "a", null));
            l.add(value(args[0], name + "b", null));
            return l;
        }
        Object o = raw.getDeclaredConstructor().newInstance();
        fill(o, Set.of());
        return o;
    }

    static List<String> diff(Object a, Object b, Set<String> skip) throws Exception {
        List<String> out = new ArrayList<>();
        diff("", a, b, skip, out);
        return out;
    }

    private static void diff(String path, Object a, Object b, Set<String> skip, List<String> out) throws Exception {
        if (a == null || b == null) {
            if (a != b) out.add(path + " (null mismatch)");
            return;
        }
        if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
            if (!ma.keySet().equals(mb.keySet())) out.add(path + " (keys)");
            else for (Object k : ma.keySet()) diff(path + "[" + k + "]", ma.get(k), mb.get(k), Set.of(), out);
            return;
        }
        if (a instanceof List<?> la && b instanceof List<?> lb) {
            if (la.size() != lb.size()) out.add(path + " (size)");
            else for (int i = 0; i < la.size(); i++) diff(path + "[" + i + "]", la.get(i), lb.get(i), Set.of(), out);
            return;
        }
        if (a.getClass().getName().startsWith("java.") || a.getClass().isEnum() || a.getClass().isPrimitive()) {
            if (!a.equals(b)) out.add(path + " (" + a + " != " + b + ")");
            return;
        }
        for (Field f : fields(a.getClass())) {
            if (skip.contains(f.getName())) continue;
            f.setAccessible(true);
            diff(path + "." + f.getName(), f.get(a), f.get(b), Set.of(), out);
        }
    }

    private static List<Field> fields(Class<?> c) {
        List<Field> out = new ArrayList<>();
        for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) out.add(f);
        }
        return out;
    }
}
