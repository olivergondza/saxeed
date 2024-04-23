package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import com.github.olivergondza.saxeed.internal.CharChunk;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharactersTest {
    @Test
    void update() {
        UpdatingVisitor capitalize = new UpdatingVisitor() {
            @Override
            public void chars(Tag.Chars tag, CharChunk chars) {
                if (tag.isNamed("r")) {
                    chars.update(chars.get().toUpperCase());
                }
            }
        };

        assertEquals(
                "<r>FOO<n>bar</n>BAX</r>",
                Util.transform("<r>foo<n>bar</n>bax</r>", capitalize, "r", "n")
        );
    }

    @Test
    void erase() {
        UpdatingVisitor eraser = new UpdatingVisitor() {
            @Override
            public void chars(Tag.Chars tag, CharChunk currentChars) {
                if (tag.isNamed("r")) {
                    currentChars.clear();
                }
            }
        };

        assertEquals(
                "<r><n>bar</n></r>",
                Util.transform("<r>foo<n>bar</n>bax</r>", eraser, "r", "n")
        );
    }

    @Test
    void chunked() throws IOException {
        List<String> out = new ArrayList<>();
        UpdatingVisitor chars = new UpdatingVisitor() {
            @Override
            public void chars(Tag.Chars tag, CharChunk currentChars) {
                out.add(currentChars.get());
            }
        };

        String input = Files.readString(Path.of("src/test/resources/xml/valid/long_body.xml"));
        Util.transform(input, chars, "root");
        assertTrue(out.size() > 10, "The large text content was broken to parts");

        assertEquals(input, "<root>" + String.join("", out) + "</root>\n");
    }

    @Test
    void writeChars() {
        UpdatingVisitor eraser = new UpdatingVisitor() {
            @Override
            public void chars(Tag.Chars tag, CharChunk currentChars) {
                assertEquals("input", currentChars.get());
            }

            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                tag.addText("Starting! ");
            }

            @Override
            public void endTag(Tag.End tag) throws FailedTransforming {
                tag.addText(" Ending!");
            }
        };

        assertEquals(
                "<r>Starting! input Ending!</r>",
                Util.transform("<r>input</r>", eraser, "r")
        );
    }

    @Test
    void repeatedTransform() {
        class Appender implements UpdatingVisitor {
            private final String content;

            public Appender(String content) {
                this.content = content;
            }

            @Override
            public void chars(Tag.Chars tag, CharChunk currentChars) {
                currentChars.update(currentChars.get() + content);
            }
        }

        TransformationBuilder tb = new TransformationBuilder()
                .add("r", new Appender(" will"))
                .add("r", new Appender(" be"))
                .add("r", new Appender(" amended"))
                .add("r", new Appender(" with"))
                .add("r", new Appender(" this"))
        ;

        assertEquals(
                "<r>Input will be amended with this</r>",
                Util.transform("<r>Input</r>", tb)
        );
    }
}
