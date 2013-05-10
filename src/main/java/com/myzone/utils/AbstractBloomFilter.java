package com.myzone.utils;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author: myzone
 * @date: 10.05.13 3:15
 */
public abstract class AbstractBloomFilter<T> extends AbstractSet<T> {

    protected final Set<T> decorated;

    public AbstractBloomFilter(@NotNull Set<T> decorated) {
        this.decorated = decorated;
    }

    protected abstract boolean isAbsent(Object o);

    protected abstract void afterAdd(T o);

    protected abstract void afterRemove(Object o);

    protected abstract void afterClear();

    @Override
    public boolean contains(Object o) {
        if (isAbsent(o))
            return false;

        return decorated.contains(o);
    }

    public boolean add(T o) {
        boolean added = decorated.add(o);

        if (added) {
            afterAdd(o);
        }

        return added;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = decorated.remove(o);

        if (removed) {
            afterRemove(o);
        }

        return removed;
    }

    @Override
    public void clear() {
        decorated.clear();

        afterClear();
    }

    @Override
    public int size() {
        return decorated.size();
    }

    @Override
    public boolean isEmpty() {
        return decorated.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return decorated.iterator();
    }

    @Override
    public Object[] toArray() {
        return decorated.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return decorated.toArray(a);
    }

    @Override
    public boolean equals(Object o) {
        return decorated.equals(o);
    }

    @Override
    public int hashCode() {
        return decorated.hashCode();
    }
}
