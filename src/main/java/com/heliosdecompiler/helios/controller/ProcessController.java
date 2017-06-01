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

package com.heliosdecompiler.helios.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Singleton
public class ProcessController {
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Process> processes = new ArrayList<>();

    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    public Process launchProcess(ProcessBuilder launch) throws IOException {
        Process process = launch.start();
        try {
            lock.lock();
            processes.add(process);
        } finally {
            lock.unlock();
        }
        backgroundTaskHelper.submit(new BackgroundTask(Message.TASK_LAUNCH_PROCESS.format(launch.command().stream().collect(Collectors.joining(" "))), true, () -> {
            try {
                process.waitFor();
                if (!process.isAlive()) {
                    processes.remove(process);
                }
            } catch (InterruptedException ignored) {
            }
        }, () -> {
            process.destroyForcibly();
            try {
                lock.lock();
                processes.remove(process);
            } finally {
                lock.unlock();
            }
        }));
        return process;
    }

    public void clear() {
        try {
            lock.lock();
            processes.forEach(p -> {
                p.destroyForcibly();
            });
            processes.clear();
        } finally {
            lock.unlock();
        }
    }
}
