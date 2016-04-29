package com.heliosdecompiler.helios.utils;

public class OSUtils {
    // http://www.java-gaming.org/index.php/topic,14110
    public static OS getOS() {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            osName = osName.toLowerCase();
            if (osName.contains("windows")) {
                return OS.WINDOWS;
            } else if (osName.contains("mac")) {
                return OS.MAC;
            } else if (osName.contains("linux")) {
                return OS.LINUX;
            } else if (osName.contains("sunos")) {
                return OS.SUNOS;
            } else if (osName.contains("freebsd")) {
                return OS.FREEBSD;
            }
        }
        return OS.UNKNOWN;
    }

    public enum OS {
        WINDOWS,
        MAC,
        LINUX,
        SUNOS,
        FREEBSD,
        UNKNOWN
    }
}
