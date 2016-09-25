package com.heliosdecompiler.helios.utils;

import java.util.function.Consumer;

public class Either<A, B> {
    private A left = null;
    private B right = null;

    private Either(A a, B b) {
        left = a;
        right = b;
    }

    public static <A, B> Either<A, B> left(A a) {
        return new Either<>(a, null);
    }

    public static <A, B> Either<A, B> right(B b) {
        return new Either<>(null, b);
    }

    public void fold(Consumer<A> error, Consumer<B> success) {
        if (right == null)
            error.accept(left);
        else
            success.accept(right);
    }

    public A left() {
        return this.left;
    }

    public B right() {
        return this.right;
    }
}
