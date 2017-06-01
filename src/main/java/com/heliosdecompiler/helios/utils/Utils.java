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

import java.util.Collection;
import java.util.Iterator;

public class Utils {
    public static <T> T find(T needle, Collection<T> haystack) {
        for (Iterator<T> iter = haystack.iterator(); iter.hasNext(); ) {
            T next = iter.next();
            if (next.equals(needle)) {
                return next;
            }
        }
        return null;
    }
}
