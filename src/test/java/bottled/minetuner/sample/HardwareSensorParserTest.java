package bottled.minetuner.sample;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pure parsers used by the LibreHardwareMonitor bridge: LHM's "45.2 °C"-style value
 * strings and its SensorId paths. These were previously private with no coverage.
 */
class HardwareSensorParserTest {

    // --- parseLeadingNumber ---

    @Test
    void nullYieldsNull() {
        assertNull(HardwareSensorPoller.parseLeadingNumber(null));
    }

    @Test
    void emptyYieldsNull() {
        assertNull(HardwareSensorPoller.parseLeadingNumber(""));
        assertNull(HardwareSensorPoller.parseLeadingNumber("   "));
    }

    @Test
    void plainTemperatureReading() {
        assertEquals(45.2, HardwareSensorPoller.parseLeadingNumber("45.2 °C"));
    }

    @Test
    void clockReading() {
        assertEquals(1234.0, HardwareSensorPoller.parseLeadingNumber("1234 MHz"));
    }

    @Test
    void percentReading() {
        assertEquals(87.5, HardwareSensorPoller.parseLeadingNumber("87.5 %"));
    }

    @Test
    void negativeReading() {
        assertEquals(-5.0, HardwareSensorPoller.parseLeadingNumber("-5 °C"));
    }

    @Test
    void explicitPlusSignIsAccepted() {
        assertEquals(3.5, HardwareSensorPoller.parseLeadingNumber("+3.5"));
    }

    @Test
    void commaDecimalSeparatorIsNormalized() {
        assertEquals(45.2, HardwareSensorPoller.parseLeadingNumber("45,2 °C"));
    }

    @Test
    void nonNumericTextYieldsNull() {
        assertNull(HardwareSensorPoller.parseLeadingNumber("NaN"));
        assertNull(HardwareSensorPoller.parseLeadingNumber("°C"));
        assertNull(HardwareSensorPoller.parseLeadingNumber("-"));
    }

    @Test
    void leadingNumberIsTruncatedAtFirstNonNumeric() {
        assertEquals(12.0, HardwareSensorPoller.parseLeadingNumber("12abc"));
        assertEquals(12.0, HardwareSensorPoller.parseLeadingNumber(" 12 x 34 "));
    }

    // --- sensorTypeFromId ---

    @Test
    void nullIdYieldsNull() {
        assertNull(HardwareSensorPoller.sensorTypeFromId(null));
        assertNull(HardwareSensorPoller.sensorTypeFromId(""));
    }

    @Test
    void nvidiaGpuTemperatureId() {
        assertEquals("temperature", HardwareSensorPoller.sensorTypeFromId("/gpu-nvidia/0/temperature/0"));
    }

    @Test
    void loadId() {
        assertEquals("load", HardwareSensorPoller.sensorTypeFromId("/gpu-nvidia/0/load/0"));
    }

    @Test
    void cpuTemperatureIdWithIndex() {
        assertEquals("temperature", HardwareSensorPoller.sensorTypeFromId("/intelcpu/0/temperature/2"));
    }

    @Test
    void shortIdStillResolvesPenultimateSegment() {
        assertEquals("x", HardwareSensorPoller.sensorTypeFromId("x/y"));
    }

    @Test
    void singleSegmentIdYieldsNull() {
        assertNull(HardwareSensorPoller.sensorTypeFromId("/"));
        assertNull(HardwareSensorPoller.sensorTypeFromId("only"));
    }
}
