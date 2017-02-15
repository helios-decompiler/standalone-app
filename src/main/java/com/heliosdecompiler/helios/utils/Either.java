/*
 * Copyright 2017 Sam Sun <github-contact@samczsun.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
