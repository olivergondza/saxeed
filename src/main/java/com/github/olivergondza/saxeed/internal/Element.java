package com.github.olivergondza.saxeed.internal;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Element introduced by visitor to be added to the output document.
 */
public interface Element {
    /**
     * Element that can write itself to XMLStreamWriter.
     */
    interface SelfWriting extends Element {
        void write(XMLStreamWriter writer) throws XMLStreamException;
    }

    final class TextString implements SelfWriting {
        private final String text;

        public TextString(String text) {
            this.text = text;
        }

        @Override
        public void write(XMLStreamWriter writer) throws XMLStreamException {
            writer.writeCharacters(text);
        }
    }
}
