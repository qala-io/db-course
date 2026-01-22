package io.qala.dbcourse.drivers;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;

public class Utils {
    private static final String DB_URL_ENV = "DB_URL";

    public static Connection connect(Map<String, String> overrideDefaults) throws Exception {
        Properties props = combinedWithDefaults(overrideDefaults);
        return DriverManager.getConnection(env("DB_URL"), props);
    }

    public static ComboPooledDataSource dbPool(Map<String, String> overrideDefaults) throws Exception {
        ComboPooledDataSource pool = new ComboPooledDataSource();
        pool.setDriverClass("org.postgresql.Driver");
        pool.setJdbcUrl(env(DB_URL_ENV));
        pool.setProperties(combinedWithDefaults(overrideDefaults));
        return pool;
    }

    private static @NonNull Properties combinedWithDefaults(Map<String, String> overrideDefaults) {
        Properties props = new Properties();
        props.put("user", env("DB_USERNAME", "postgres"));
        props.put("password", env("DB_PASSWORD", "postgres"));
        props.put("currentSchema", env("DB_DEFAULT_SCHEMA", "public"));
        for (Map.Entry<String, String> e : overrideDefaults.entrySet())
            props.setProperty(e.getKey(), e.getValue());
        return props;
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
