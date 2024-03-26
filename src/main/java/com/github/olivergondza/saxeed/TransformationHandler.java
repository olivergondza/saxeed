package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedWriting;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SAX Handler to apply visitors to elements and generate resulting document.
 */
public class TransformationHandler extends DefaultHandler implements AutoCloseable {

    public static final String ERROR_WRITING_TO_OUTPUT_FILE = "Error writing to output file";

    /**
     * List of visitors to perform operation for every tag.
     * New instances create for every file, so they can be stateful.
     */
    private final Map<String, List<Visitor>> visitors;
    private final Set<Visitor> uniqueVisitors;

    public interface Visitor {

        default void startElement(TagVisit tag) {}
        default void body(TagVisit tag) {}
        default void endElement(TagVisit tag) {}
        default void endDocument() throws SAXException {}

        default Element newElement(String name, Map<String, String> attrs) {
            Element element = DocumentHelper.createElement(name);
            for (Map.Entry<String, String> a : attrs.entrySet()) {
                element.addAttribute(a.getKey(), a.getValue());
            }
            return element;
        }

        default Element newElement(String name, List<Element> children) {
            Element element = DocumentHelper.createElement(name);
            for (Element child : children) {
                element.add(child);
            }
            return element;
        }

        default Element newElement(String name) {
            return DocumentHelper.createElement(name);
        }
    }

    private final XMLStreamWriter writer;
    private TagVisit currentTag;

    public TransformationHandler(Map<String, List<Visitor>> visitors, XMLStreamWriter writer) {
        this.visitors = visitors;
        this.writer = writer;

        uniqueVisitors = this.visitors.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private TagVisit enterTag(String tagname, Attributes attributes) {
        currentTag = new TagVisit(currentTag, tagname, attributes);
        return currentTag;
    }

    @Override
    public void startElement(String uri, String localName, String tagname, Attributes attributes) throws SAXException {
        TagVisit tag = enterTag(tagname, attributes);
        _startElement(tag);
    }

    private void _startElement(TagVisit tag) throws SAXException {
        if (tag.isSkipped()) return;

        List<Visitor> visitors = this.visitors.get(tag.getName());
        if (visitors != null) {
            for (Visitor v : visitors) {
                v.startElement(tag);

                // TODO: fatal error and file deletion needs redesigning

                if (tag.isSkipped()) return;
            }
        }

        Element sw = tag.getSurroundWith();

        if (sw != null) {

            // The hierarchy of tag parents needs to be fixed as we have injected a new one
            // between `currentTag` and `currentTag.parent`
            tag.parent = new TagVisit(tag.parent, sw.getName(), getSaxAttributes(sw));
            _startElement(tag.parent);

            if (!Objects.equals(sw.getTextTrim(), "")) {
                throw new AssertionError("Writing text content not supported for added tags");
            }

            if (!sw.content().isEmpty()) {
                throw new AssertionError(
                        "Writing sub-elements is not supported for suround with elements: " + sw.content()
                );
            }
        }

        try {
            writer.writeStartElement(tag.getName());
            for (Map.Entry<String, String> e : tag.getAttributes().entrySet()) {
                writer.writeAttribute(e.getKey(), e.getValue());
            }

            writeTagsRecursively(tag.getTagsAdded());
            tag.getTagsAdded().clear();
        } catch (XMLStreamException e) {
            throw new SAXException(ERROR_WRITING_TO_OUTPUT_FILE, e);
        }
    }

    /**
     * Write now tags to output stream.
     *
     * This is implemented through call XUnitFixer's handler methods, so Fixup impls are aware of newly added tags.
     */
    private void writeTagsRecursively(List<Element> tagsAdded) throws SAXException {
        if (tagsAdded.isEmpty()) return;

        for (Element element : tagsAdded) {

            currentTag = new TagVisit(currentTag, element.getName(), getSaxAttributes(element), true);
            _startElement(currentTag);
            if (!Objects.equals(element.getTextTrim(), "")) {
                throw new AssertionError("Writing text content not supported for added tags");
            }

            for (Node node : element.content()) {
                if (!(node instanceof Element)) throw new AssertionError(
                        "Writing non-Elements is not supported. Given: " + node.getClass()
                );
                writeTagsRecursively(List.of((Element) node));
            }
            endElement(null, null, element.getName());
        }
    }

    private static AttributesImpl getSaxAttributes(Element element) {
        AttributesImpl attrs = new AttributesImpl();
        for (Attribute src : element.attributes()) {
            attrs.addAttribute(null, null, src.getName(), "string", src.getValue());
        }
        return attrs;
    }

    @Override
    public void endElement(String uri, String localName, String tagname) throws SAXException {
        if (!Objects.equals(tagname, currentTag.getName())) {
            throw new AssertionError("Ending element " + tagname + " while inside " + currentTag.getName());
        }

        TagVisit tag = currentTag;
        currentTag = currentTag.parent;

        if (!tag.isSkipped()) {
            List<Visitor> visitors = this.visitors.get(tagname);
            if (visitors != null) {
                for (Visitor v : visitors) {
                    v.endElement(tag);
                }
            }

            writeTagsRecursively(tag.getTagsAdded());
            tag.getTagsAdded().clear();

            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new SAXException(ERROR_WRITING_TO_OUTPUT_FILE, e);
            }
        }

        Element sw = tag.getSurroundWith();
        if (sw != null) {
            endElement(null, null, sw.getName());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        TagVisit tag = currentTag;

        if (tag != null && !tag.isSkipped()) {
            try {
                writer.writeCharacters(ch, start, length);
            } catch (XMLStreamException e) {
                throw new SAXException(ERROR_WRITING_TO_OUTPUT_FILE, e);
            }
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        try {
            writer.writeCharacters(ch, start, length);
        } catch (XMLStreamException e) {
            throw new SAXException(ERROR_WRITING_TO_OUTPUT_FILE, e);
        }
    }

    @Override
    public void endDocument() throws SAXException {

        for (Visitor visitor: this.uniqueVisitors) {
            visitor.endDocument();
        }

        close();
    }

    @Override
    public void close() throws FailedWriting {
        try {
            writer.close();
        } catch (XMLStreamException e) {
            throw new FailedWriting("Failed closing writer stream", e);
        }
    }

    public static final class DeleteFileException extends SAXException {
        public DeleteFileException(String message) {
            super(message);
        }
    }
}
