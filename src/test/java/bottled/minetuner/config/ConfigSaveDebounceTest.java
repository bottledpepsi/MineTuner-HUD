package bottled.minetuner.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The debounced-save lifecycle: editor mutations request a save, the tick hook
 * flushes it at most once per window, and flushSave forces it immediately. This
 * replaces the old behavior of a synchronous whole-file write per click.
 */
class ConfigSaveDebounceTest {

    @TempDir
    Path dir;

    private Path file;

    @BeforeEach
    void setUp() throws IOException {
        file = dir.resolve("minetuner.json");
        MineTunerConfig.SAVE_DEBOUNCE_MS = 0; // tests flush on the first tickSave
        MineTunerConfig.setConfigPathSupplier(() -> file);
        Files.writeString(file, "{\"overlayEnabled\": true}");
        MineTunerConfig.load();
    }

    @AfterEach
    void tearDown() {
        MineTunerConfig.SAVE_DEBOUNCE_MS = 1000;
        MineTunerConfig.setConfigPathSupplier(() -> {
            throw new IllegalStateException("config path not set");
        });
    }

    private String fileText() throws IOException {
        return Files.readString(file);
    }

    @Test
    void requestSaveFlushesOnTheNextTickAfterTheWindow() throws IOException {
        MineTunerConfig cfg = MineTunerConfig.getInstance();
        cfg.overlayEnabled = false;
        cfg.requestSave();
        cfg.tickSave(); // window is 0 in tests -> flushes now
        assertTrue(fileText().contains("\"overlayEnabled\": false"),
                "the debounced change must reach disk on the next tickSave");
    }

    @Test
    void pendingChangeSurvivesMultipleTickCallsUntilTheWindowElapses() throws IOException {
        MineTunerConfig.SAVE_DEBOUNCE_MS = 60_000; // effectively never during this test
        MineTunerConfig cfg = MineTunerConfig.getInstance();
        cfg.overlayEnabled = false;
        cfg.requestSave();
        cfg.tickSave();
        cfg.tickSave();
        assertFalse(fileText().contains("\"overlayEnabled\": false"),
                "inside the debounce window nothing may be written yet");
        cfg.flushSave();
        assertTrue(fileText().contains("\"overlayEnabled\": false"),
                "flushSave must write the pending change immediately");
    }

    @Test
    void tickSaveWithoutAPendingChangeWritesNothing() throws IOException {
        String before = fileText();
        MineTunerConfig.getInstance().tickSave();
        org.junit.jupiter.api.Assertions.assertEquals(before, fileText());
    }
}
