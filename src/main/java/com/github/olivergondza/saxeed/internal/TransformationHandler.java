package com.github.olivergondza.saxeed.internal;

import com.github.olivergondza.saxeed.Subscribed;
import com.github.olivergondza.saxeed.Tag;
import com.github.olivergondza.saxeed.UpdatingVisitor;
import com.github.olivergondza.saxeed.ex.FailedTransforming;
import com.github.olivergondza.saxeed.ex.FailedWriting;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    // A resource to close once done. Can be null
    private final AutoCloseable closeAction;
    private TagImpl currentTag;

    public TransformationHandler(
            LinkedHashMap<UpdatingVisitor, Subscribed> visitors,
            XMLStreamWriter writer,
            AutoCloseable closeAction
    ) {
        this.visitors = visitors;
        this.writer = writer;
        this.closeAction = closeAction;
    }

    private TagImpl enterTag(String tagname, Attributes attributes) {
        currentTag = new TagImpl(currentTag, tagname, attributes);
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

        TagImpl wrapper = tag.startWrapWith();
        if (wrapper != null) {
            if (!(wrapper.getTagsAdded().isEmpty())) {
                throw new AssertionError(
                        "Writing sub-elements is not supported for suround with elements: " + wrapper.getTagsAdded()
                );
            }

            _startElement(wrapper);
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
    private void writeTagsRecursively(List<Tag> tagsAdded) {
        if (tagsAdded.isEmpty()) return;

        TagImpl oldCurrentTag = currentTag;
        for (Tag tag: tagsAdded) {

            currentTag = (TagImpl) tag;
            _startElement(currentTag);


            for (Tag child: currentTag.getTagsAdded()) {
                writeTagsRecursively(List.of(child));
            }
            endElement(null, null, currentTag.getName());
        }
        currentTag = oldCurrentTag;
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

        TagImpl ww = tag.endWrapWith();
        if (ww != null) {
            endElement(null, null, ww.getName());
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
    }

    @Override
    public void close() throws FailedWriting {
        try {
            // Make sure the content is written to XMLStreamWriter, no matter if closing the target or not
            writer.flush();

            // Closing XMLStreamWriter never close the target, but it will be un-writable afterward.
            // So closing writer iff the target should be closed.
            if (closeAction != null) {
                // Hackish: Need to close 2 resources, and need to preserve both the eventual exceptions from close() - exactly what
                // an empty try-with-resources would do. But, the XMLStreamWriter does not implement AutoClosable, so a compromise
                // approach is needed.
                try (closeAction) {
                    writer.close();
                }
            }
        } catch (Exception e) {
            throw new FailedWriting("Failed closing stream", e);
        }
    }

    public static final class DeleteFileException extends FailedTransforming {
        public DeleteFileException(String message) {
            super(message);
        }
    }
}
