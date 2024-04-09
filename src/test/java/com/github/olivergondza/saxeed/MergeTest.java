package com.github.olivergondza.saxeed;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeTest {
    @Test
    void merge() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(baos);

        writer.writeStartElement("root");

        new Saxeed().setInputString("<saxeed1/>").addTransformation(new TransformationBuilder(), writer).transform();

        writer.writeStartElement("middle");
        writer.writeEndElement();

        new Saxeed().setInputString("<saxeed2/>").addTransformation(new TransformationBuilder(), writer).transform();

        writer.writeEndElement();

        assertEquals("<root><saxeed1></saxeed1><middle></middle><saxeed2></saxeed2></root>", baos.toString());
    }
}
