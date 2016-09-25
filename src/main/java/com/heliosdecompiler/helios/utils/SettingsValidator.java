package com.heliosdecompiler.helios.utils;

import com.heliosdecompiler.helios.Settings;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SettingsValidator {
    private static Boolean python2Verified = null;
    private static Boolean javaRtVerified = null;

    public static Result ensurePython2Set() {
        return ensurePython2Set(false);
    }

    public static Result ensurePython2Set(boolean forceCheck) {
        String python2Location = Settings.PYTHON2_LOCATION.get().asString();
        if (python2Location.isEmpty()) {
            return Result.NO_SETTING_SPECIFIED.create();
        }
        if (python2Verified == null || forceCheck) {
            try {
                Process process = new ProcessBuilder(python2Location, "-V").start();
                String result = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
                String error = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
                python2Verified = error.startsWith("Python 2") || result.startsWith("Python 2");
            } catch (Throwable t) {
                t.printStackTrace();
                return Result.ERROR_OCCURED.create(t);
            }
        }
        return python2Verified ? Result.SUCCESS.create() : Result.INVALID_SETTING.create();
    }

    public static Result ensureJavaRtSet() {
        return ensureJavaRtSet(false);
    }

    public static Result ensureJavaRtSet(boolean forceCheck) {
        String javaRtLocation = Settings.RT_LOCATION.get().asString();
        if (javaRtLocation.isEmpty()) {
            return Result.NO_SETTING_SPECIFIED.create();
        }
        if (javaRtVerified == null || forceCheck) {
            ZipFile zipFile = null;
            try {
                File rtjar = new File(javaRtLocation);
                if (rtjar.exists()) {
                    zipFile = new ZipFile(rtjar);
                    ZipEntry object = zipFile.getEntry("java/lang/Object.class");
                    if (object != null) {
                        javaRtVerified = true;
                    }
                }
            } catch (Throwable t) {
                return Result.INVALID_SETTING.create(t);
            } finally {
                IOUtils.closeQuietly(zipFile);
                if (javaRtVerified == null) {
                    javaRtVerified = false;
                }
            }
        }
        return javaRtVerified ? Result.SUCCESS.create() : Result.INVALID_SETTING.create();
    }
}
