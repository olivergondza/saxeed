package com.github.olivergondza.saxeed.internal;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class CharChunk {
    private char[] origData;
    private int origStart;
    private int origLength;

    /**
     * Updated content.
     */
    private String replacement;

    public CharChunk() {
    }

    public void update(char[] charsData, int charsStart, int charsLength) {
        origData = charsData;
        origStart = charsStart;
        origLength = charsLength;
        replacement = null;
    }

    public void update(String text) {
        replacement = text;
        origData = null;
        origStart = -1;
        origLength = -1;
    }

    public void clear() {
        update(null);
    }

    public boolean isEmpty() {
        return origData == null && replacement == null;
    }

    public String get() {
        if (replacement != null) {
            return replacement;
        }

        // cleared
        if (origData == null) {
            return null;
        }

        // Construct the String iff some visitor really need a String instance.
        // This is to prevent data copying/allocation that might not be needed.
        replacement = new String(origData, origStart, origLength);
        return replacement;
    }

    /*package*/ void write(XMLStreamWriter writer) throws XMLStreamException {
        // origData are erased when content is updated
        if (origData != null) {
            writer.writeCharacters(origData, origStart, origLength);
        } else {
            writer.writeCharacters(replacement);
        }
    }
}
