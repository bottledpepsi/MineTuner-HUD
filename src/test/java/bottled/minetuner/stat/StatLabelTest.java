package bottled.minetuner.stat;

import com.google.gson.Gson;
import net.minecraft.client.resources.language.I18n;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The declared-label contract: every stat whose en_us format string carries a
 * "Label: " prefix must ship a matching {@code minetuner.label.<token>} key, so
 * showPrefix=off and template mode strip the prefix via the declared label
 * (translation-order independent) instead of matching ": " inside the translation.
 * A drift between the two would silently stop stripping for that stat.
 *
 * <p>The pairing itself is verified against the en_us.json file directly: the test
 * environment doesn't load mod translations into I18n (only the real game does),
 * so the runtime resolution path is asserted separately and skipped when the
 * language manager has no mod translations loaded.
 */
class StatLabelTest {

    @SuppressWarnings("unchecked")
    private static Map<String, String> enUs() {
        InputStream in = StatLabelTest.class.getResourceAsStream("/assets/minetuner/lang/en_us.json");
        assertNotNull(in, "en_us.json must be on the test classpath");
        return (Map<String, String>) new Gson().fromJson(
                new InputStreamReader(in, StandardCharsets.UTF_8), Map.class);
    }

    @Test
    void everyRegisteredLabelPairsWithAFormatString() {
        // For each registered stat's label key, SOME stat format string must start
        // with exactly "<label>: " — the runtime prefix-strip contract, checked
        // against the file (a stat's format key suffix can differ from its token,
        // so the pairing is by text, not by key name).
        var formats = enUs().entrySet().stream()
                .filter(e -> e.getKey().startsWith("minetuner.stat."))
                .toList();
        int checked = 0;
        for (StatDefinition def : StatRegistry.all()) {
            String labelKey = "minetuner.label." + def.token();
            String label = enUs().get(labelKey);
            if (label == null) continue; // stat without a declared label: ": " fallback
            boolean paired = formats.stream().anyMatch(e -> e.getValue().startsWith(label + ": "));
            assertTrue(paired, labelKey + " (\"" + label + "\") matches no stat format string");
            checked++;
        }
        assertTrue(checked > 40, "expected to verify a meaningful number of labels, got " + checked);
    }

    @Test
    void everyLabeledFormatStringPrefixIsCoveredBySomeLabel() {
        // Reverse direction: a stat format string with a "Label: " prefix must have
        // SOME label key with that exact text (otherwise that stat relies on the
        // legacy fallback unintentionally).
        var labels = enUs().entrySet().stream()
                .filter(e -> e.getKey().startsWith("minetuner.label."))
                .map(Map.Entry::getValue)
                .toList();
        for (Map.Entry<String, String> e : enUs().entrySet()) {
            if (!e.getKey().startsWith("minetuner.stat.")) continue;
            String value = e.getValue();
            int sep = value.indexOf(": ");
            if (sep <= 0 || value.substring(0, sep).contains("%s")) continue;
            assertTrue(labels.contains(value.substring(0, sep)),
                    e.getKey() + " has label prefix \"" + value.substring(0, sep)
                            + "\" but no minetuner.label.* key with that text");
        }
    }

    @Test
    void declaredLabelsResolveAndPrefixTheirFormatOutputWhenTranslationsAreLoaded() {
        // Skip (not fail) in environments where mod translations aren't loaded into
        // I18n — the game always loads them; bare fabric-loader-junit may not.
        String probe = I18n.get("minetuner.stat.fps");
        Assumptions.assumeTrue(!probe.equals("minetuner.stat.fps"),
                "mod translations not loaded into I18n in this environment");
        for (StatDefinition def : StatRegistry.all()) {
            String label = def.label();
            if (label == null) continue;
            String formatted = def.format(def.defaultDecimals());
            if (!formatted.contains(": ")) continue; // unavailable right now ("") — nothing to check
            assertTrue(formatted.startsWith(label + ": "),
                    def.key() + "'s declared label \"" + label
                            + "\" must match its format output \"" + formatted + "\"");
        }
    }

    @Test
    void labelKeysNeverContainTheSeparator() {
        for (Map.Entry<String, String> e : enUs().entrySet()) {
            if (e.getKey().startsWith("minetuner.label.")) {
                assertTrue(!e.getValue().contains(": "),
                        e.getKey() + " must not include the separator");
            }
        }
    }
}
