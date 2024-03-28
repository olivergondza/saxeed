package com.github.olivergondza.saxeed;

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
    private final Map<String, List<UpdatingVisitor>> visitors;
    private final Set<UpdatingVisitor> uniqueVisitors;

    private final XMLStreamWriter writer;
    private TagImpl currentTag;

    public TransformationHandler(Map<String, List<UpdatingVisitor>> visitors, XMLStreamWriter writer) {
        this.visitors = visitors;
        this.writer = writer;

        uniqueVisitors = this.visitors.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
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

        List<UpdatingVisitor> visitors = this.visitors.get(tag.getName());
        if (visitors != null) {
            for (UpdatingVisitor v : visitors) {
                v.startTag(tag);

                if (tag.isOmitted()) return;
            }
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
            List<UpdatingVisitor> visitors = this.visitors.get(tagname);
            if (visitors != null) {
                for (UpdatingVisitor v : visitors) {
                    v.endTag(tag);
                }
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

        for (UpdatingVisitor visitor: this.uniqueVisitors) {
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
