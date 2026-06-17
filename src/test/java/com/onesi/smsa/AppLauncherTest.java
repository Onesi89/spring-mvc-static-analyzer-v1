package com.onesi.smsa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AppLauncherTest {
    @Test
    void launchesGuiWhenNoArgumentsAreProvided() {
        AtomicBoolean guiLaunched = new AtomicBoolean(false);
        AppLauncher launcher = new AppLauncher(() -> guiLaunched.set(true));

        int exitCode = launcher.launch(new String[0]);

        assertThat(exitCode).isEqualTo(0);
        assertThat(guiLaunched).isTrue();
    }

    @Test
    void usesCliWhenArgumentsAreProvided() {
        AtomicBoolean guiLaunched = new AtomicBoolean(false);
        AppLauncher launcher = new AppLauncher(() -> guiLaunched.set(true));

        int exitCode = launcher.launch(new String[] {"missing-directory"});

        assertThat(exitCode).isEqualTo(1);
        assertThat(guiLaunched).isFalse();
    }
}
