package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedWriting;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify targets are (not) closed as documented.
 */
class ClosingTest {

    private static final class ClosingTarget extends Target {

        private final List<Throwable> closes = new ArrayList<>();
        private final AutoCloseable closeTracker;

        public ClosingTarget(boolean close) {
            closeTracker = close
                    ? (() -> closes.add(new Throwable()))
                    : null
            ;
        }

        @Override
        public String getName() {
            return "test fake";
        }

        @Override
        public XMLStreamWriter getWriter(Saxeed saxeed) {
            registerClosable(closeTracker);
            return createXmlStreamWriter(new ByteArrayOutputStream());
        }

        public void assertClosedTimes(int expected) {
            int count = closes.size();
            Supplier<String> msg = () -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                ps.printf("Close is expected to be called %d times, was %d.", expected, count);

                for (Throwable throwable : closes) {
                    ps.println();
                    throwable.printStackTrace(ps);
                }

                return baos.toString();
            };
            assertEquals(expected, count, msg);
        }
    }

    @Test
    void closeFileTarget() throws Exception {
        final boolean[] closed = {false};
        Path out = Files.createTempFile("saxeed", getClass().getName());
        Target.FileTarget target = new Target.FileTarget(out) {
            @Override
            protected OutputStream getOutputStream() throws FailedWriting {
                return new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {

                    }

                    @Override
                    public void close() throws IOException {
                        closed[0] = true;
                    }
                };
            }
        };

        new Saxeed().setInputString("<saxeed/>").addTransformation(new TransformationBuilder(), target).transform();

        assertTrue(closed[0]);
    }

    @Test
    void doNotCloseOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void close() {
                throw new AssertionError("Must not close this!");
            }
        };
        baos.write("<open>".getBytes());

        new Saxeed().setInputString("<saxeed/>").addTransformation(new TransformationBuilder(), baos).transform();

        baos.write("</open>".getBytes());

        assertEquals("<open><saxeed></saxeed></open>", baos.toString());
    }

    @Test
    void doNotCloseXmlStreamWriterTarget() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter writer = Mockito.spy(Target.createXmlStreamWriter(baos));
        Mockito.doThrow(new AssertionError("Stream closed"))
                .when(writer).close();

        Target.XmlStreamWriterTarget target = new Target.XmlStreamWriterTarget(writer);

        writer.writeStartElement("open");
        new Saxeed().setInputString("<saxeed/>").addTransformation(new TransformationBuilder(), target).transform();
        writer.writeEndElement();

        assertEquals("<open><saxeed></saxeed></open>", baos.toString());
    }
}
