package com.github.olivergondza.saxeed;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeTest {
    @Test
    void merge() throws Exception {
        ByteArrayOutputStream os = Mockito.spy(new ByteArrayOutputStream());
        XMLStreamWriter writer = Mockito.spy(Target.createXmlStreamWriter(os));
        try (os) {

            writer.writeStartElement("root");

            new Saxeed().setInputString("<saxeed1/>").addTransformation(new TransformationBuilder(), writer).transform();
            Mockito.verify(writer, Mockito.never()).close();
            Mockito.verify(writer, Mockito.times(1)).flush();

            writer.writeStartElement("middle");
            writer.writeEndElement();

            new Saxeed().setInputString("<saxeed2/>").addTransformation(new TransformationBuilder(), writer).transform();
            Mockito.verify(writer, Mockito.never()).close();
            Mockito.verify(writer, Mockito.times(2)).flush();

            writer.writeEndElement();
            assertEquals("<root><saxeed1></saxeed1><middle></middle><saxeed2></saxeed2></root>", os.toString());

            // Saxeed does not close the output stream
            Mockito.verify(os, Mockito.never()).close();
        } finally {
            // try-with-resources does
            Mockito.verify(os, Mockito.times(1)).close();
        }
    }
}
