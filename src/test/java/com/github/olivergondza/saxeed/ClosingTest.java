package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.internal.TransformationHandler;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify targets are (not) closed as documented.
 */
class ClosingTest {

    private static final class ClosingTarget implements Target, AutoCloseable {

        private final boolean close;
        private final List<Throwable> closes = new ArrayList<>();

        public ClosingTarget(boolean close) {
            this.close = close;
        }

        @Override
        public String getName() {
            return "test fake";
        }

        @Override
        public TransformationHandler getHandler(TransformationBuilder builder, XMLOutputFactory xmlOutputFactory) {
            XMLStreamWriter writer = create(xmlOutputFactory, new ByteArrayOutputStream());
            return builder.build(writer, close ? this : null);
        }

        @Override
        public void close() throws Exception {
            this.closes.add(new Throwable());
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
    void close() {
        ClosingTarget closeMe = new ClosingTarget(true);
        new Saxeed().setInputString("<saxeed/>")
                .addTransformation(new TransformationBuilder(), closeMe)
                .transform();

        closeMe.assertClosedTimes(1);

        closeMe = new ClosingTarget(true);
        new Saxeed().setInputString("<saxeed/>")
                .addTransformation(new TransformationBuilder(), closeMe)
                .addTransformation(new TransformationBuilder(), closeMe)
                .addTransformation(new TransformationBuilder(), closeMe)
                .transform();

        closeMe.assertClosedTimes(3);
    }

    @Test
    void doNotClose() {
        ClosingTarget doNotCloseMe = new ClosingTarget(false);
        new Saxeed().setInputString("<saxeed/>").addTransformation(new TransformationBuilder(), doNotCloseMe).transform();

        doNotCloseMe.assertClosedTimes(0);
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
}
