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

package com.heliosdecompiler.helios.api.events;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Events {
    private static final ExecutorService EVENT_LOOP = Executors.newFixedThreadPool(4, r -> new Thread(r, "Event Thread"));

    private static final Map<Class<?>, Method> METHOD_MAP;

    private static final List<Listener> listeners = new ArrayList<>();

    static {
        Map<Class<?>, Method> methodMap = new HashMap<>();
        for (Method method : Listener.class.getMethods()) {
            if (method.getDeclaringClass() == Listener.class) {
                methodMap.put(method.getParameterTypes()[0], method);
            }
        }
        METHOD_MAP = Collections.unmodifiableMap(methodMap);
    }

    public static void registerListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static <T> void callEvent(T consume) {
        EVENT_LOOP.submit(() -> {
            try {
                Method method = METHOD_MAP.get(consume.getClass());
                if (method != null) {
                    synchronized (listeners) {
                        for (Listener listener : listeners) {
                            try {
                                method.invoke(listener, consume);
                            } catch (Throwable e) {
//                                ExceptionHandler.handle(e);
                            }
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unknown event " + consume.getClass());
                }
            } catch (Exception e) {
//                ExceptionHandler.handle(e);
            }
        });
    }
}
