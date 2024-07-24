package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedWriting;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Target to write the resulting content into.
 */
public abstract class Target implements AutoCloseable {

    private AutoCloseable close = null;

    /**
     * Identify the target for the ease of debugging.
     *
     * Mostly used in error messages to identify the file/stream/etc.
     */
    public abstract String getName();

    /**
     * Create new XMLStreamWriter for the transformation.
     *
     * If it is desirable to close any resource allocated, register it by {@link #registerClosable(AutoCloseable)}.
     */
    public abstract XMLStreamWriter getWriter(Saxeed saxeed);

    public static XMLStreamWriter createXmlStreamWriter(OutputStream os) {
        try {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(os);
        } catch (FactoryConfigurationError | XMLStreamException e) {
            throw new FailedWriting("Unable to create XMLStreamWriter from " + objectId(os), e);
        }
    }

    // Get native toString, to name objects we do not know much about
    private static String objectId(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
    }

    /**
     * Register single resource for closing on {@link #close()}.
     */
    protected void registerClosable(AutoCloseable close) {
        if (this.close != null) throw new IllegalStateException("Unclosed resource already present: " + this.close);

        this.close = close;
    }

    /**
     * Close method is called on target every time a transformation using a target is completed.
     *
     * It closes whatever resource was registered using {@link #registerClosable(AutoCloseable)}, or nothing if not set.
     */
    @Override
    public void close() throws FailedWriting {
        if (close != null) {
            try {
                close.close();
                close = null;
            } catch (Exception e) {
                throw new FailedWriting("Failed closing target " + getName(), e);
            }
        }
    }

    /**
     * Target for a file.
     *
     * The content is flush/closed once the transformation is over. It means that using same File target repeatedly will
     * overwrite its content.
     */
    static class FileTarget extends Target {
        private final File file;

        public FileTarget(File file) {
            this.file = file;
        }

        public FileTarget(Path path) {
            this.file = path.toFile();
        }

        @Override
        public String getName() {
            return file.getAbsolutePath();
        }

        @Override
        public XMLStreamWriter getWriter(Saxeed saxeed) {
            OutputStream os = getOutputStream();
            registerClosable(os);
            return createXmlStreamWriter(os);
        }

        protected /*for testing*/ OutputStream getOutputStream() throws FailedWriting {
            try {
                return new BufferedOutputStream(new FileOutputStream(file));
            } catch (FileNotFoundException e) {
                throw new FailedWriting("Cannot create/open file: " + file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Simple target wrapping plain old OutputStream.
     *
     * The stream is NOT closed.
     */
    static class OutputStreamTarget extends Target {
        private final OutputStream os;

        public OutputStreamTarget(OutputStream os) {
            this.os = os;
        }

        @Override
        public String getName() {
            return "explicit OutputStream " + objectId(os);
        }

        @Override
        public XMLStreamWriter getWriter(Saxeed saxeed) {
            return createXmlStreamWriter(os);
        }
    }

    static class DevNullTarget extends OutputStreamTarget {

        private static final OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) {
                // noop
            }
        };

        public DevNullTarget() {
            super(outputStream);
        }

        @Override
        public String getName() {
            return "target /dev/null";
        }
    }

    static class XmlStreamWriterTarget extends Target {

        private final XMLStreamWriter writer;

        public XmlStreamWriterTarget(XMLStreamWriter writer) {
            this.writer = writer;
        }

        @Override
        public String getName() {
            return "explicit writer " + objectId(writer);
        }

        @Override
        public XMLStreamWriter getWriter(Saxeed saxeed) {
            return writer;
        }
    }
}
