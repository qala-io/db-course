package io.qala.dbcourse.drivers;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;

public class Utils {

    public static Connection connect(Map<String, String> overrideDefaults) throws Exception {
        Properties props = new Properties();
        props.put("user", env("DB_USERNAME", "postgres"));
        props.put("password", env("DB_PASSWORD", "postgres"));
        props.put("currentSchema", env("DB_DEFAULT_SCHEMA", "public"));
        String url = env("DB_URL");
        for (Map.Entry<String, String> e : overrideDefaults.entrySet())
            props.setProperty(e.getKey(), e.getValue());
        return DriverManager.getConnection(url, props);
    }
    public static String env(String propName) {
        String result = System.getenv(propName);
        if(result == null)
            throw new IllegalStateException("Required env var isn't set: " + propName);
        return result;
    }
    public static String env(String propName, String defaultVal) {
        String result = System.getenv(propName);
        return result != null ? result : defaultVal;
    }
    public static <T> T getField(Object o, String fieldName) {
        Field field = findFieldInHierarchy(o.getClass(), fieldName);
        try {
            //noinspection unchecked
            return (T) field.get(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static Field findFieldInHierarchy(Class<?> c, String fieldName) {
        Class<?> clazz = c;
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if(field == null)
            throw new RuntimeException("Couldn't find field " + fieldName + " in " + c + " or its superclasses");
        field.setAccessible(true);
        return field;
    }
}
