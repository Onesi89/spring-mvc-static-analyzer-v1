package com.onesi.smsa;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new AppLauncher().launch(args);
        if (args.length > 0) {
            System.exit(exitCode);
        }
    }
}
