package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedWriting;
import com.github.olivergondza.saxeed.internal.TransformationHandler;

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
public interface Target {

    /**
     * Identify the target for the ease of debugging.
     *
     * Mostly used in error messages to identify the file/stream/etc.
     */
    String getName();

    TransformationHandler getHandler(TransformationBuilder builder, XMLOutputFactory xmlOutputFactory);

    default XMLStreamWriter create(XMLOutputFactory xmlOutputFactory, OutputStream os) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(os);
        } catch (XMLStreamException e) {
            throw new FailedWriting("Unable to create XMLStreamWriter for " + getName(), e);
        }
    }

    // Get native toString, to name objects we do not know much about
    private static String objectId(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
    }

    /**
     * Target for a file.
     *
     * The content is flush/closed once the transformation is over. It means that using same File target repeatedly will
     * overwrite its content.
     */
    class FileTarget implements Target {
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
        public TransformationHandler getHandler(TransformationBuilder builder, XMLOutputFactory xmlOutputFactory) {
            OutputStream os = getOutputStream();
            return builder.build(create(xmlOutputFactory, os), os);
        }

        private OutputStream getOutputStream() throws FailedWriting {
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
    class OutputStreamTarget implements Target {
        private final OutputStream os;

        public OutputStreamTarget(OutputStream os) {
            this.os = os;
        }

        @Override
        public String getName() {
            return "explicit OutputStream " + objectId(os);
        }

        @Override
        public TransformationHandler getHandler(TransformationBuilder builder, XMLOutputFactory xmlOutputFactory) {
            return builder.build(create(xmlOutputFactory, os), null);
        }
    }

    class DevNullTarget extends OutputStreamTarget {

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

    class XmlStreamWriterTarget implements Target {

        private final XMLStreamWriter writer;

        public XmlStreamWriterTarget(XMLStreamWriter writer) {
            this.writer = writer;
        }

        @Override
        public String getName() {
            return "explicit writer " + objectId(writer);
        }

        @Override
        public TransformationHandler getHandler(TransformationBuilder builder, XMLOutputFactory __) {
            return builder.build(writer, null);
        }
    }
}
