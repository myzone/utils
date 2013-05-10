package com.myzone.utils;

import com.google.common.base.Predicate;

/**
 * @author : myzone
 * @date: 18.04.13 19:24
 */
public final class PredicateUtils {

    public static void doIf(boolean condition, Runnable runnable) {
        if (condition) {
            runnable.run();
        }
    }

    private PredicateUtils() {}

}
