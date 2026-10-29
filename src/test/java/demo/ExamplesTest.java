package demo;

import demo.single.SingleFileExamples;
import demo.cross.CrossFileExamples;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ExamplesTest {
    private final SingleFileExamples single = new SingleFileExamples();
    private final CrossFileExamples cross = new CrossFileExamples();

    @Test void fileUriRoundTripsInBothLayouts() throws Exception {
        Path expected = Path.of("sample.txt").toAbsolutePath();
        String json = "\"" + expected.toUri() + "\"";
        assertEquals(expected, single.readPath(json));
        assertEquals(expected, cross.readPath(json));
    }

    @Test void xmlDurationHasExpectedComponentsInBothLayouts() throws Exception {
        for (var duration : new javax.xml.datatype.Duration[]{
                single.readDuration("\"P2DT3H\""), cross.readDuration("\"P2DT3H\"")}) {
            assertEquals(2, duration.getDays());
            assertEquals(3, duration.getHours());
        }
    }

    @Test void unknownTypeNameUsesFallbackInBothLayouts() throws Exception {
        String json = "{\"kind\":\"example\",\"message\":\"hello\"}";
        assertEquals("hello", single.readEvent(json).message);
        assertEquals("hello", cross.readEvent(json).message);
    }

    @Test void freshBufferProducesExpectedPaddingInBothLayouts() throws Exception {
        assertArrayEquals(new byte[4], single.writePadding(4));
        assertArrayEquals(new byte[4], cross.writePadding(4));
        assertEquals(0, single.writePadding(0).length);
        assertEquals(0, cross.writePadding(0).length);
    }

    @Test void directPathDeserializerRejectsNonStringTokensInBothLayouts() {
        assertThrows(com.fasterxml.jackson.databind.exc.MismatchedInputException.class,
                () -> single.readPath("123"));
        assertThrows(com.fasterxml.jackson.databind.exc.MismatchedInputException.class,
                () -> cross.readPath("123"));
    }

    @Test void malformedInputsAreRejectedInBothLayouts() {
        assertThrows(Exception.class, () -> single.readDuration("\"invalid\""));
        assertThrows(Exception.class, () -> cross.readDuration("\"invalid\""));
        assertThrows(IllegalArgumentException.class, () -> single.writePadding(65));
        assertThrows(IllegalArgumentException.class, () -> cross.writePadding(-1));
    }
}
