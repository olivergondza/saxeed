package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.internal.CharChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify we can process unicode in chars correctly.
 */
class UnicodeTest {

    @ParameterizedTest
    @MethodSource("unicode")
    void roundtrip(Path input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        new Saxeed().addTransformation(new TransformationBuilder(), baos)
                .setInput(input)
                .transform()
        ;

        assertEquals(
                normalizeEntities(Files.readString(input).trim()),
                baos.toString()
        );
    }

    @ParameterizedTest
    @MethodSource("unicode")
    void parsableChars(Path input) throws IOException {
        String plain = normalizeEntities(Files.readString(input).trim());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        class Visitor implements UpdatingVisitor {
            @Override
            public void chars(Tag.Chars tag, CharChunk chars) {
                // Verify that the chunk of characters is not chopped inside unicode character
                assertTrue(plain.contains(chars.get()));
            }
        }

        TransformationBuilder tb = new TransformationBuilder()
                .add(Subscribed.toAll(), new Visitor())
        ;

        new Saxeed().addTransformation(tb, baos)
                .setInput(input)
                .transform()
        ;
    }

    static String normalizeEntities(String in) {
        return in.replace("&#x26;", "&amp;")
                .replace("&#x3C;", "&lt;")
        ;
    }

    static Stream<Path> unicode() throws IOException {
        Path root = Path.of("src/test/resources/unicode");
        return Files.list(root);
    }
}
