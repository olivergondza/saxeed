package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedReading;
import com.github.olivergondza.saxeed.ex.FailedTransforming;
import com.github.olivergondza.saxeed.ex.FailedWriting;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Saxeed {

    private SAXParser saxParser;
    private XMLOutputFactory xmlOutputFactory;
    private InputSource input;
    private final Map<TransformationBuilder, Target> transformations = new LinkedHashMap<>();

    public Saxeed() {
        setXmlOutputFactory(null); // Initialize with defaults
    }

    public Saxeed setSaxParser(SAXParser saxParser) {
        this.saxParser = saxParser;

        return this;
    }

    public Saxeed setXmlOutputFactory(XMLOutputFactory xmlOutputFactory) {
        this.xmlOutputFactory = xmlOutputFactory != null
                ? xmlOutputFactory
                : XMLOutputFactory.newInstance()
        ;
        return this;
    }

    public Saxeed setInput(Path path) {
        input = new InputSource(path.toFile().toURI().toASCIIString());
        return this;
    }

    public Saxeed setInput(java.io.File file) {
        input = new InputSource(file.toURI().toASCIIString());
        return this;
    }

    public Saxeed setInputUri(String uri) {
        input = new InputSource(uri);
        return this;
    }

    public Saxeed setInputString(String xml) {
        input = new InputSource(new StringReader(xml));
        input.setSystemId("In-memory string");
        return this;
    }

    private Saxeed addTransformation(TransformationBuilder transformation, Target target) {
        transformations.put(transformation, target);
        return this;
    }

    public Saxeed addTransformation(TransformationBuilder transformation, Path path) {
        return addTransformation(transformation, new Target.FileTarget(path));
    }

    public Saxeed addTransformation(TransformationBuilder transformation, File file) {
        return addTransformation(transformation, new Target.FileTarget(file));
    }

    public Saxeed addTransformation(TransformationBuilder transformation, OutputStream os) {
        return addTransformation(transformation, new Target.OutputStreamTarget(os));
    }

    /**
     * Perform the configured transformation.
     *
     * @throws FailedReading When problem reading the supplied input.
     * @throws FailedTransforming When some of the visitors fails.
     * @throws FailedWriting When some of the targets failed writing.
     */
    public void transform() throws FailedReading, FailedTransforming, FailedWriting {
        validateConfig();

        // Stream process the file to a temp destination
        try (MultiplexingHandler handler = getSaxHandler()) {
            getSaxParser().parse(input, handler);
        } catch (IOException ex) {
            throw new FailedWriting("Failed reading input file", ex);
        } catch (SAXParseException ex) {
            // Provide more descriptive error message for parsing errors
            String msg = ex.getMessage().replace("[.]$", "");
            throw new FailedReading(String.format(
                    "Failed parsing input file: %s at [%d:%d]", msg, ex.getLineNumber(), ex.getColumnNumber()
            ), ex);
        } catch (SAXException ex) {
            throw new FailedTransforming("Failed processing input file", ex);
        }
    }

    private void validateConfig() throws IllegalStateException {
        if (input == null) throw new IllegalStateException("No input data configured");
        if (transformations.isEmpty()) throw new IllegalStateException("No transformations configured");
    }

    private SAXParser getSaxParser() {
        if (saxParser != null) return saxParser;

        // We can add schema validation here for extra defensiveness.
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            return factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new AssertionError("SAX or its essential features are not supported", e);
        }
    }

    private MultiplexingHandler getSaxHandler() {
        List<TransformationHandler> handlers = new ArrayList<>(transformations.size());
        for (Map.Entry<TransformationBuilder, Target> trans : transformations.entrySet()) {
            Target target = trans.getValue();
            try {
                XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(target.getOutputStream());
                handlers.add(trans.getKey().build(writer));
            } catch (XMLStreamException e) {
                throw new FailedWriting("Unable to create XML Stream Writer for: " + target.getName(), e);
            }
        }
        return new MultiplexingHandler(handlers);
    }
}
