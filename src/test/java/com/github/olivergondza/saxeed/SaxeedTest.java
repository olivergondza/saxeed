package com.github.olivergondza.saxeed;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SaxeedTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("SaxeedTest");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> list = Files.list(tempDir)) {
            list.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.deleteIfExists(tempDir);
    }

    @Test
    void asIs() throws Exception {
        class DeleteAll implements TransformationHandler.Visitor {
            @Override
            public void startElement(TagVisit tag) {
                tag.skip();
            }
        }

        String f = "src/test/resources/xml/valid/cd_catalog.xml";
        Path input = Path.of(f);
        Path output = tempDir.resolve(input.getFileName());

        TransformationBuilder transformation = new TransformationBuilder()
                .put("nosuchtagused", new DeleteAll())
        ;

        Saxeed saxeed = new Saxeed()
            .setInput(input)
            .addTransformation(transformation, output)
        ;

        saxeed.transform();

        assertEquals(Files.readString(input).trim(), Files.readString(output));
    }
}
