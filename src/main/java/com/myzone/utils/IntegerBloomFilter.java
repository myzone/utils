package com.myzone.utils;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : myzone
 * @date: 10.05.13 3:34
 */
public class IntegerBloomFilter<T> extends AbstractBloomFilter<T> {

    protected final AtomicInteger mask;

    public IntegerBloomFilter(@NotNull Set<T> decorated) {
        super(decorated);

        mask = new AtomicInteger(getMask(decorated));
    }

    @Override
    protected boolean isAbsent(Object o) {
        int hash = ObjectUtils.hashCode(o);

        return hash != (hash & mask.get());
    }

    @Override
    protected void afterAdd(T o) {
        while (true) {
            int oldMask = mask.get();
            int updatedMask = addToMask(oldMask, o);

            if (mask.compareAndSet(oldMask, updatedMask)) {
                break;
            }
        }
    }

    @Override
    protected void afterRemove(Object o) {
        while (true) {
            int oldMask = mask.get();
            int updatedMask = getMask(this.decorated);

            if (mask.compareAndSet(oldMask, updatedMask)) {
                break;
            }
        }
    }

    @Override
    protected void afterClear() {
        while (true) {
            int oldMask = mask.get();
            int updatedMask = 0;

            if (mask.compareAndSet(oldMask, updatedMask)) {
                break;
            }
        }
    }

    protected int addToMask(int mask, Object o) {
        return mask | ObjectUtils.hashCode(o);
    }

    protected int getMask(@NotNull Set<?> set) {
        int mask = 0;

        for (Object o : set) {
            mask = addToMask(mask, o);
        }

        return mask;
    }
}
