/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.utility;

import java.util.Properties;

import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */
public final class SystemPropertyUtil {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(SystemPropertyUtil.class);

    private static final Properties props;

    // Retrieve all system properties at once so that there's no need to deal with
    // security exceptions from next time.  Otherwise, we might end up with logging every
    // security exceptions on every system property access or introducing more complexity
    // just because of less verbose logging.
    static {
        Properties newProps = null;
        try {
            newProps = System.getProperties();
        } catch (SecurityException e) {
            logger.warn("Unable to access the system properties; default values will be used.", e);
            newProps = new Properties();
        }

        props = newProps;
    }

    /**
     * Returns {@code true} if and only if the system property with the specified {@code key}
     * exists.
     */
    public static boolean contains(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return props.containsKey(key);
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
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static String get(String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static boolean getBoolean(String key, boolean def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.length() == 0) {
            return true;
        }

        if (value.equals("true") || value.equals("yes") || value.equals("1")) {
            return true;
        }

        if (value.equals("false") || value.equals("no") || value.equals("0")) {
            return false;
        }

        logger.warn(
                "Unable to parse the boolean system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static int getInt(String key, int def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                // Ignore
            }
        }

        logger.warn(
                "Unable to parse the integer system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static long getLong(String key, long def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                // Ignore
            }
        }

        logger.warn(
                "Unable to parse the long integer system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    private SystemPropertyUtil() {
        // Unused
    }
}
