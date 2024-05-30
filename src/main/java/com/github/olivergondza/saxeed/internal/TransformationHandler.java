package com.github.olivergondza.saxeed.internal;

import com.github.olivergondza.saxeed.Subscribed;
import com.github.olivergondza.saxeed.TagName;
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
    private final Map<TagName, List<UpdatingVisitor>> visitorCache = new HashMap<>();

    private final XMLStreamWriter writer;
    // A resource to close once done. Can be null
    private final AutoCloseable closeAction;

    private TagImpl currentTag;
    private final CharChunk currentChars = new CharChunk();
    private LinkedHashMap<String, String> currentNsMapping = new LinkedHashMap<>();

    private Map<String, String> documentNamespaces = new HashMap<>();

    public TransformationHandler(
            LinkedHashMap<UpdatingVisitor, Subscribed> visitors,
            XMLStreamWriter writer,
            AutoCloseable closeAction
    ) {
        this.visitors = visitors;
        this.writer = writer;
        this.closeAction = closeAction;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
        documentNamespaces.put(uri, prefix);
        currentNsMapping.put(uri, prefix);
    }

    @Override
    public void startElement(String uri, String localName, String tagName, Attributes attributes) {
        TagName tagname = TagName.fromSaxArgs(uri, localName, tagName);
        currentTag = new TagImpl(currentTag, tagname, attributes, currentNsMapping);
        currentNsMapping.clear();

        _startElement(currentTag);
    }

    private void _startElement(TagImpl tag) {
        if (tag.isOmitted()) return;

        TagName name = tag.getName();
        for (UpdatingVisitor v : getVisitors(name)) {
            v.startTag(tag);

            if (tag.isOmitted()) return;
        }

        TagImpl wrapper = tag.startWrapWith();
        if (wrapper != null) {
            List<Element> children = wrapper.consumeChildren();
            if (!(children.isEmpty())) {
                throw new AssertionError(
                        "Writing sub-tags is not supported for wrapWith tags: " + children
                );
            }

            // Wrapping root tag with named namespaces declared. Cary them to the new root tag.
            Map<String, String> namespaces = tag.getNamespaces();
            if (wrapper.getParent() == null && !namespaces.isEmpty()) {
                for (Map.Entry<String, String> e : namespaces.entrySet()) {
                    wrapper.declareNamespace(e.getKey(), e.getValue());
                }
                namespaces.clear();
            }

            _startElement(wrapper);
        }

        try {
            LOGGER.fine("<" + name);

            // Make sure that eventual Tag.Start#declareNamespace() additions are reflected
            documentNamespaces.putAll(tag.getNamespaces());

            boolean usesNamespace = name.getNsUri().isEmpty();
            if (usesNamespace) {
                writer.writeStartElement(name.getLocal());
            } else {
                String declaredPrefix = documentNamespaces.get(name.getNsUri());
                if (declaredPrefix == null) {
                    throw new FailedTransforming(
                            "Unable to write tag (" + tag.getName() + "), no such namespace URI declared. Have: " + documentNamespaces
                    );
                }
                if (!Objects.equals(declaredPrefix, name.getNsPrefix())) {
                    throw new FailedTransforming(
                            "Unable to write tag (" + tag.getName() + "), no such namespace URI+prefix declared. Prefix: " + declaredPrefix
                    );
                }
                writer.writeStartElement(name.getNsPrefix(), name.getLocal(), name.getNsUri());
            }

            writeNamespaceDeclarations(tag);

            for (Map.Entry<String, String> e : tag.getAttributes().entrySet()) {
                LOGGER.fine(String.format("%s='%s'", e.getKey(), e.getValue()));
                writer.writeAttribute(e.getKey(), e.getValue());
            }
            LOGGER.fine(">");

            writeChildren(tag);
        } catch (XMLStreamException e) {
            throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
        }
    }

    /**
     * Write namespace declarations ("xmlns" pseudo-attributes), existing or added
     */
    private void writeNamespaceDeclarations(TagImpl tag) throws XMLStreamException {
        for (Map.Entry<String, String> e : tag.getNamespaces().entrySet()) {
            writer.writeNamespace(e.getValue(), e.getKey());
        }
    }

    /**
     * Get visitors subscribed to given tag name.
     *
     * The result is cached.
     */
    private List<UpdatingVisitor> getVisitors(TagName tagName) {
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
     * <p>
     * This is implemented through call XUnitFixer's handler methods, so Fixup impls are aware of newly added tags.
     */
    private boolean writeChildren(TagImpl tag) {
        List<Element> childElements = tag.consumeChildren();
        if (childElements.isEmpty()) return false;

        TagImpl oldCurrentTag = currentTag;
        for (Element elements: childElements) {

            if (elements instanceof TagImpl) {
                currentTag = (TagImpl) elements;
                _startElement(currentTag);

                writeChildren(currentTag);
                TagName tn = currentTag.getName();
                endElement(tn.getNsUri(), tn.getLocal(), tn.getNsPrefix());
            } else if (elements instanceof Element.SelfWriting) {
                Element.SelfWriting sw = (Element.SelfWriting) elements;
                try {
                    sw.write(writer);
                } catch(XMLStreamException ex) {
                    throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, ex);
                }
            } else {
                throw new AssertionError("Unknown Element implementation found: " + elements.getClass());
            }
        }
        currentTag = oldCurrentTag;
        return true;
    }

    @Override
    public void endElement(String uri, String localName, String tagname) {
        if (currentTag == null) throw new AssertionError("Closing tag without currentTag set");

        if (!Objects.equals(localName, currentTag.getName().getLocal())) {
            throw new AssertionError("Ending element " + tagname + " while inside " + currentTag.getName());
        }

        TagImpl tag = currentTag;

        if (!tag.isOmitted()) {
            List<UpdatingVisitor> visitors = getVisitors(tag.getName());
            // Iterate reversed for closing tag
            for (int i = visitors.size() - 1; i >= 0; i--) {
                visitors.get(i).endTag(tag);
            }

            writeChildren(tag);

            try {
                LOGGER.fine("</" + tagname + ">");
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
            }
        }

        currentTag = (TagImpl) currentTag.getParent();

        TagImpl ww = tag.endWrapWith();
        if (ww != null) {
            TagName wwName = ww.getName();
            endElement(wwName.getNsUri(), wwName.getLocal(), wwName.getQualifiedName());
        }
    }

    @Override
    public void characters(char[] orig, int start, int length) {
        TagImpl tag = currentTag;

        if (tag != null && !tag.isCharactersOmitted()) {
            try {
                currentChars.update(orig, start, length);
                for (UpdatingVisitor visitor : getVisitors(tag.getName())) {
                    visitor.chars(tag, currentChars);
                }

                boolean written = writeChildren(tag);
                if (!currentChars.isEmpty()) {
                    if (written) {
                        throw new IllegalStateException(
                                "Unable to write characters and children at the same time. "
                                + "Make sure to call CharChunk#clear() when elements added in UpdatingVisitor#chars()"
                        );
                    }
                    currentChars.write(writer);
                }
            } catch (XMLStreamException e) {
                throw new FailedWriting(ERROR_WRITING_TO_OUTPUT_FILE, e);
            } finally {
                currentChars.clear();
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
    public void startDocument() {
        for (UpdatingVisitor visitor: this.visitors.keySet()) {
            visitor.startDocument();
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
