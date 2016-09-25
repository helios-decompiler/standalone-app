/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
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

package com.heliosdecompiler.helios.handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BackgroundTaskHandler {
    // todo pls
    public static final BackgroundTaskHandler INSTANCE = new BackgroundTaskHandler();

    private final ExecutorService executor = Executors.newFixedThreadPool(8, r -> new Thread(r, "Background Thread"));

    private volatile int runningTasks = 0;
    private volatile long currentTaskId = 0;

    @SuppressWarnings("deprecated")
    public Future<?> submit(Runnable runnable) {
        return executor.submit(() -> {
            runningTasks++;
//            System.out.println("BEGIN EXECUTING " + System.identityHashCode(runnable));
            Thread thread = new Thread(runnable, System.identityHashCode(runnable) + " - " + currentTaskId++);
            thread.setUncaughtExceptionHandler((t, e) -> ExceptionHandler.handle(e));
            thread.start();
            try {
//                System.out.println("JOINING " + System.identityHashCode(runnable));
                thread.join();
            } catch (InterruptedException ex) {
                try {
                    thread.suspend();
                    thread.stop();
                } catch (ThreadDeath ignored) {
                    // Don't care
                }
            } catch (Throwable t) {
                ExceptionHandler.handle(t);
            }
//            System.out.println("DONE EXECUTING " + System.identityHashCode(runnable));
//            while (thread.isAlive()) {
//                StackTraceElement[] e = Thread.getAllStackTraces().get(thread);
//                System.out.println(e.length + " " + Arrays.toString(e));
//            }
//            System.out.println("THREAD DIED " + System.identityHashCode(runnable));
            runningTasks--;
        });
    }

    public void shutdown() {
        System.out.println("Shutting down executors...");
        executor.shutdownNow();
        System.out.println("Done shutting down executors");
    }

    public int getActiveTasks() {
        return runningTasks;
    }
}
