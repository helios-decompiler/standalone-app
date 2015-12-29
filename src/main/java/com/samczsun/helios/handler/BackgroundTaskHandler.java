/*
 * Copyright 2015 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.samczsun.helios.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundTaskHandler {
    private final ExecutorService executor = Executors.newFixedThreadPool(8, r -> new Thread(r, "Background Thread"));
    private final ExecutorService synchronizer = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "Synchronizer Thread"));

    private final Set<Runnable> tasks = new HashSet<>(); //This should only be modified by the synchronizer

    public void submit(Runnable runnable) {
        if (!(runnable instanceof WrappedRunnable)) {
            submit(new WrappedRunnable(runnable));
        } else {
            this.synchronizer.execute(() -> {
                if (tasks.add(runnable)) {
                    executor.execute(() -> {
                        runnable.run();
                        synchronizer.execute(() -> tasks.remove(runnable));
                    });
                }
            });
        }
    }

    public void shutdown() {
        System.out.println("Shutting down executors...");
        executor.shutdownNow();
        synchronizer.shutdownNow();
        System.out.println("Done shutting down executors");
    }

    public int getActiveTasks() {
        return tasks.size();
    }

    private static class WrappedRunnable implements Runnable {
        private final Runnable wrapped;

        WrappedRunnable(Runnable wrapped) {
            this.wrapped = wrapped;
        }

        public void run() {
            wrapped.run();
        }
    }
}
