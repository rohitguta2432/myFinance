package com.myfinance.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class EnumUtils {

    private EnumUtils() {}

    public static <E extends Enum<E>> E safeEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            log.warn("enum.parse.unknown value='{}' type={}", value, clazz.getSimpleName());
            return null;
        }
    }

    public static String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
