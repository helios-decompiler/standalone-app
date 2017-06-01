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

package com.heliosdecompiler.helios.controller.backgroundtask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.ui.MessageHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class BackgroundTaskHelper {
    private final AtomicInteger threadId = new AtomicInteger(0);
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> new Thread(r, "Background Thread #" + threadId.getAndIncrement()));

    private ObservableList<BackgroundTask> runningInstances = FXCollections.observableArrayList();
    private ObservableList<BackgroundTask> runningInstancesShow = FXCollections.observableArrayList();

    private volatile int runningTasks = 0;
    private volatile long currentTaskId = 0;

    @Inject
    private MessageHandler messageHandler;

    @SuppressWarnings("deprecated")
    public Future<?> submit(BackgroundTask runnable) {
        Future<?> future = executor.submit(() -> {
            runningInstances.add(runnable);
            if (runnable.isShow()) {
                runningInstancesShow.add(runnable);
            }
            runningTasks++;
//            System.out.println("BEGIN EXECUTING " + System.identityHashCode(runnable));
            Thread thread = new Thread(runnable, System.identityHashCode(runnable) + " - " + currentTaskId++);
            thread.setUncaughtExceptionHandler((t, e) -> messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), e));
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
                messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), t);
            }
//            System.out.println("DONE EXECUTING " + System.identityHashCode(runnable));
//            while (thread.isAlive()) {
//                StackTraceElement[] e = Thread.getAllStackTraces().get(thread);
//                System.out.println(e.length + " " + Arrays.toString(e));
//            }
//            System.out.println("THREAD DIED " + System.identityHashCode(runnable));
            runningTasks--;
            runningInstances.remove(runnable);
            runningInstancesShow.remove(runnable);
        });
        runnable.init(future);
        return future;
    }

    public void shutdown() {
        System.out.println("Shutting down executors...");
        executor.shutdownNow();
        System.out.println("Done shutting down executors");
    }

    public int getActiveTasks() {
        return runningTasks;
    }

    public ObservableList<BackgroundTask> runningInstances() {
        return runningInstances;
    }

    public ObservableList<BackgroundTask> visibleRunningInstances() {
        return runningInstancesShow;
    }

    public List<BackgroundTask> getTasks(boolean all) {
        if (all) {
            return runningInstances;
        } else {
            return runningInstancesShow;
        }
    }
}
