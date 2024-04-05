package com.github.olivergondza.saxeed.internal;

import com.github.olivergondza.saxeed.Subscribed;
import com.github.olivergondza.saxeed.UpdatingVisitor;
import com.github.olivergondza.saxeed.ex.FailedTransforming;
import com.github.olivergondza.saxeed.ex.FailedWriting;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * SAX Handler to apply visitors to elements and generate resulting document.
 */
public class TransformationHandler extends DefaultHandler implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(TransformationHandler.class.getName());

    public static final String ERROR_WRITING_TO_OUTPUT_FILE = "Error writing to output file";

    /**
     * List of visitors to perform operation for every tag.
     * New instances create for every file, so they can be stateful.
     */
    private final LinkedHashMap<UpdatingVisitor, Subscribed> visitors;
    private final Map<String, List<UpdatingVisitor>> visitorCache = new HashMap<>();

    private final XMLStreamWriter writer;
    private TagImpl currentTag;

    public TransformationHandler(LinkedHashMap<UpdatingVisitor, Subscribed> visitors, XMLStreamWriter writer) {
        this.visitors = visitors;
        this.writer = writer;
    }

    private TagImpl enterTag(String tagname, Attributes attributes) {
        currentTag = new TagImpl(currentTag, tagname, attributes, false);
        return currentTag;
    }

    @Override
    public void startElement(String uri, String localName, String tagname, Attributes attributes) {
        TagImpl tag = enterTag(tagname, attributes);
        _startElement(tag);
    }

    private void _startElement(TagImpl tag) {
        if (tag.isOmitted()) return;

        for (UpdatingVisitor v : getVisitors(tag.getName())) {
            v.startTag(tag);

            if (tag.isOmitted()) return;
        }

        Element sw = tag.getWrapWith();

        if (sw != null) {

            TagImpl wrapper = tag.wrapInto(sw.getName(), getSaxAttributes(sw));
            _startElement(wrapper);

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
            LOGGER.fine("<" + tag.getName());
            writer.writeStartElement(tag.getName());
            for (Map.Entry<String, String> e : tag.getAttributes().entrySet()) {
                LOGGER.fine(String.format("%s='%s'", e.getKey(), e.getValue()));
                writer.writeAttribute(e.getKey(), e.getValue());
            }
            LOGGER.fine(">");

            writeTagsRecursively(tag.getTagsAdded());
            tag.getTagsAdded().clear();
        } catch (XMLStreamException e) {
            throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
        }
    }

    /**
     * Get visitors subscribed to given tag name.
     *
     * The result is cached.
     */
    private List<UpdatingVisitor> getVisitors(String tagName) {
        List<UpdatingVisitor> visitors = visitorCache.get(tagName);
        if (visitors != null) return visitors;

        visitors = this.visitors.entrySet().stream()
                .filter(e -> e.getValue().isSubscribed(tagName))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
        ;

        visitorCache.put(tagName, visitors);

        return visitors;
    }

    /**
     * Write now tags to output stream.
     *
     * This is implemented through call XUnitFixer's handler methods, so Fixup impls are aware of newly added tags.
     */
    private void writeTagsRecursively(List<Element> tagsAdded) {
        if (tagsAdded.isEmpty()) return;

        TagImpl oldCurrentTag = currentTag;
        for (Element element : tagsAdded) {

            currentTag = new TagImpl(oldCurrentTag, element.getName(), getSaxAttributes(element), true);
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
        currentTag = oldCurrentTag;
    }

    private static AttributesImpl getSaxAttributes(Element element) {
        AttributesImpl attrs = new AttributesImpl();
        for (Attribute src : element.attributes()) {
            attrs.addAttribute(null, null, src.getName(), "string", src.getValue());
        }
        return attrs;
    }

    @Override
    public void endElement(String uri, String localName, String tagname) {
        if (currentTag == null) throw new AssertionError("Closing tag without currentTag se");

        if (!Objects.equals(tagname, currentTag.getName())) {
            throw new AssertionError("Ending element " + tagname + " while inside " + currentTag.getName());
        }

        TagImpl tag = currentTag;

        if (!tag.isOmitted()) {

            List<UpdatingVisitor> visitors = getVisitors(tagname);
            // Iterate reversed for closing tag
            for (int i = visitors.size() - 1; i >= 0; i--) {
                visitors.get(i).endTag(tag);
            }

            writeTagsRecursively(tag.getTagsAdded());
            tag.getTagsAdded().clear();

            try {
                LOGGER.fine("</" + tagname + "> (saxeed: " + currentTag.getName() + ")");
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
            }
        }

        currentTag = (TagImpl) currentTag.getParent();

        Element sw = tag.getWrapWith();
        if (sw != null) {
            endElement(null, null, sw.getName());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        TagImpl tag = currentTag;

        if (tag != null && !tag.isCharactersOmitted()) {
            try {
                writer.writeCharacters(ch, start, length);
            } catch (XMLStreamException e) {
                throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
            }
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
        try {
            writer.writeCharacters(ch, start, length);
        } catch (XMLStreamException e) {
            throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
        }
    }

    @Override
    public void processingInstruction(String target, String data) {
        try {
            writer.writeProcessingInstruction(target, data);
        } catch (XMLStreamException e) {
            throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
        }
    }

    @Override
    public void endDocument() {

        for (UpdatingVisitor visitor: this.visitors.keySet()) {
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

    public static final class DeleteFileException extends FailedTransforming {
        public DeleteFileException(String message) {
            super(message);
        }
    }
}
