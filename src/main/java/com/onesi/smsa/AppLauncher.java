package com.onesi.smsa;

import com.onesi.smsa.cli.AnalyzeCommand;
import com.onesi.smsa.gui.AnalyzerGui;
import java.util.Arrays;
import picocli.CommandLine;

public class AppLauncher {
    private final Runnable guiLauncher;

    public AppLauncher() {
        this(() -> AnalyzerGui.show());
    }

    AppLauncher(Runnable guiLauncher) {
        this.guiLauncher = guiLauncher;
    }

    public int launch(String[] args) {
        if (args.length == 0) {
            guiLauncher.run();
            return 0;
        }
        return new CommandLine(new AnalyzeCommand()).execute(Arrays.copyOf(args, args.length));
    }
}
