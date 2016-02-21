package com.samczsun.helios.utils;

import java.util.Iterator;

public class MultiIterator<T> implements Iterator<T> {

    private class MultiIterable implements Iterable<T> {
        @Override
        public Iterator<T> iterator() {
            return MultiIterator.this;
        }
    }

    private int index;

    private Iterator<? extends T> current;

    private final Iterable<? extends T>[] iterables;

    private MultiIterator(Iterable<? extends T>[] iterables) {
        this.iterables = iterables;
        this.current = this.iterables[this.index].iterator();
    }

    @Override
    public boolean hasNext() {
        if (!this.current.hasNext()) {
            while (this.index + 1 < this.iterables.length && !this.current.hasNext()) {
                this.current = this.iterables[++this.index].iterator();
            }
        }
        return this.current.hasNext();
    }

    @Override
    public T next() {
        return this.current.next();
    }

    public Iterable<T> toIterable() {
        return new MultiIterable();
    }

    @SafeVarargs
    public static <T> MultiIterator<? extends T> of(Iterable<? extends T>... iterables) {
        return new MultiIterator<T>(iterables);
    }
}
