package com.nothing.remapper;

import rikka.shizuku.Shizuku;

public class ShizukuUtils {
    public static void runCommand(String[] cmd) {
        try {
            Process process = Shizuku.newProcess(cmd, null, null);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
