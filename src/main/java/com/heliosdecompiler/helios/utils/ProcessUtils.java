package com.heliosdecompiler.helios.utils;

import com.heliosdecompiler.helios.handler.BackgroundTaskHandler;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProcessUtils {
    private static final List<Process> processes = Collections.synchronizedList(new ArrayList<>());

    public static Process launchProcess(ProcessBuilder launch) throws IOException {
        Process process = launch.start();
        processes.add(process);
        BackgroundTaskHandler.INSTANCE.submit(() -> {
            try {
                process.waitFor();
                if (!process.isAlive()) {
                    processes.remove(process);
                }
            } catch (InterruptedException ignored) {
            }
        });
        return process;
    }

    public static void destroyAll() {
        processes.forEach(Process::destroy);
        processes.clear();
    }

    public static String readProcess(Process process) {
        StringBuilder result = new StringBuilder();
        result.append("--- BEGIN PROCESS DUMP ---").append("\n");
        result.append("---- STDOUT ----").append("\n");
        InputStream inputStream = process.getInputStream();
        byte[] inputStreamBytes = new byte[0];
        try {
            inputStreamBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            result.append("An error occured while reading from stdout").append("\n");
            result.append("Caused by: ").append(e.getClass()).append(" ").append(e.getMessage()).append("\n");
        } finally {
            if (inputStreamBytes.length > 0) {
                result.append(new String(inputStreamBytes, StandardCharsets.UTF_8));
            }
        }
        result.append("---- STDERR ----").append("\n");
        inputStream = process.getErrorStream();
        inputStreamBytes = new byte[0];
        try {
            inputStreamBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            result.append("An error occured while reading from stderr").append("\n");
            result.append("Caused by: ").append(e.getClass()).append(" ").append(e.getMessage()).append("\n");
        } finally {
            if (inputStreamBytes.length > 0) {
                result.append(new String(inputStreamBytes, StandardCharsets.UTF_8));
            }
        }

        result.append("---- EXIT VALUE ----").append("\n");

        int exitValue = -0xCAFEBABE;
        try {
            exitValue = process.waitFor();
        } catch (InterruptedException e) {
            result.append("An error occured while obtaining the exit value").append("\n");
            result.append("Caused by: ").append(e.getClass()).append(" ").append(e.getMessage()).append("\n");
        } finally {
            if (exitValue != -0xCAFEBABE) {
                result.append("Process finished with exit code ").append(exitValue).append("\n");
            }
        }

        return result.toString();
    }
}
