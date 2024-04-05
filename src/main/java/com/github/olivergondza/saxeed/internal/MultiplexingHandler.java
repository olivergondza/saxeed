package com.github.olivergondza.saxeed.internal;

import com.github.olivergondza.saxeed.ex.FailedWriting;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;

public class MultiplexingHandler extends DefaultHandler implements AutoCloseable {
    private final List<TransformationHandler> handlers;

    public MultiplexingHandler(List<TransformationHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void close() throws FailedWriting {
        FailedWriting exception = null;
        for (TransformationHandler handler : handlers) {
            try {
                handler.close();
            } catch (Exception ex) {
                if (exception == null) {
                    exception = new FailedWriting("Failed closing handler " + handler, ex);
                } else {
                    exception.addSuppressed(ex);
                }
            }
        }

        if (exception != null) throw exception;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        for (TransformationHandler handler : handlers) {
            handler.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.endDocument();
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.endPrefixMapping(prefix);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.startElement(uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.endElement(uri, localName, qName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.characters(ch, start, length);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.processingInstruction(target, data);
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        for (TransformationHandler handler : handlers) {
            handler.skippedEntity(name);
        }
    }
}
