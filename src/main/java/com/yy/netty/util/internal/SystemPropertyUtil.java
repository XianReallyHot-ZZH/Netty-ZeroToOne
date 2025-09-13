package com.yy.netty.util.internal;

public class SystemPropertyUtil {

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     * {@code def} if there's no such property or if an access to the
     * specified property is not allowed.
     */
    public static int getInt(String key, int def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim();
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            // Ignore
        }
        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to {@code null} if the property access fails.
     *
     * @return the property value or {@code null}
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     * {@code def} if there's no such property or if an access to the
     * specified property is not allowed.
     */
    private static String get(String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        String value = null;

        // 访问获取系统参数
        try {
            // Java 17 中安全管理器已弃用，可直接获取系统属性
            value = System.getProperty(key);
        } catch (SecurityException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (value == null) {
            return def;
        }

        return value;

    }


}
