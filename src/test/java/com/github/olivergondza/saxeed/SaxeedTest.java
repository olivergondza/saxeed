package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.olivergondza.saxeed.UpdatingVisitor.newElement;
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
        class DeleteAll implements UpdatingVisitor {
            @Override
            public void startTag(Tag.Start tag) {
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

    @Test
    void processingInstructionWritten() {
        String same = "<?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?><a></a>";
        assertEquals(same, transform(
                same,
                (ts) -> {},
                "a"
        ));
    }

    @Test
    void entity() {
        String same = "<r>&lt;<i>&amp;</i>&gt;</r>";
        assertEquals(same, transform(
                same,
                (ts) -> {},
                "r"
        ));
    }

    @Test
    void whitespace() {
        String same = "<r><s> </s> <t>\t</t>\t<nl>\n</nl>\n</r>";
        assertEquals(same, transform(
                same,
                (ts) -> {},
                "r"
        ));
    }

    @Test
    void cdata() {
        assertEquals("<r>Some\tText&amp;Here</r>", transform(
                "<r><![CDATA[Some\tText&Here]]></r>",
                (ts) -> {},
                "r"
        ));
    }

    @Test
    void skip() {
        assertEquals("", transform(
                "<root></root>",
                Tag.Start::skip,
                "root"
        ));

        assertEquals("<a></a>", transform(
                "<a><skip><child/><another></another></skip></a>",
                Tag.Start::skip,
                "skip"
        ));

        assertEquals("<a><w></w></a>", transform(
                "<a><skip><child/></skip><w><skip/></w></a>",
                Tag.Start::skip,
                "skip"
        ));
    }

    @Test
    void empty() {
        String actual = transform(
                "<a><empty><child/><another></another></empty><s></s></a>",
                Tag.Start::empty,
                "empty"
        );

        assertEquals("<a><empty></empty><s></s></a>", actual);
    }

    @Test
    void unwrap() {
        String actual = transform(
                "<a><unwrap><child/><another><unwrap><foo/></unwrap></another></unwrap></a>",
                Tag.Start::unwrap,
                "unwrap"
        );

        assertEquals("<a><child></child><another><foo></foo></another></a>", actual);
    }

    @Test
    void attributes() {
        String actual = transform(
                "<root a='v'><child a='v'></child></root>",
                ts -> {
                    ts.removeAttribute("no_such_attr");
                    ts.removeAttributes(List.of("a", "nope"));
                },
                "root"
        );

        assertEquals("<root><child a=\"v\"></child></root>", actual);
    }

    @Test
    void wrap() {
        Consumer<Tag.Start> wrap = ts -> {
            ts.wrapWith(newElement(
                    "wrapper", Map.of("a1", "v1", "a2", "v2")
            ));
        };
        assertEquals(
                "<wrapper a1=\"v1\" a2=\"v2\"><a></a></wrapper>",
                transform("<a></a>", wrap, "a")
        );

        // TODO
//        assertEquals(
//                "<wrapper a1=\"v1\" a2=\"v2\"><a><wrapper a1=\"v1\" a2=\"v2\"><a></a></wrapper></a></wrapper>",
//                transform("<a><i><a/></i></a>", wrap, "a")
//        );
    }

    @Test
    void addChildren() {
        // Add children to the beginning and to the end of the tag
        UpdatingVisitor addch = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                tag.addChildren(List.of(
                        newElement("head"),
                        newElement("neck", Map.of("a", ""))
                ));
            }

            @Override
            public void endTag(Tag.End tag) throws FailedTransforming {
                tag.addChild(newElement("tail", List.of(newElement("tail"))));
            }
        };

        assertEquals(
                "<r><head></head><neck a=\"\"></neck><existing></existing><tail><tail></tail></tail></r>",
                transform("<r><existing/></r>", addch, "r")
        );
    }

    String transform(String input, Consumer<Tag.Start> lambda, String... on) {
        UpdatingVisitor visitor = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                lambda.accept(tag);
            }
        };

        return transform(input, visitor, on);
    }

    private static String transform(String input, UpdatingVisitor visitor, String... on) {
        TransformationBuilder tb = new TransformationBuilder();
        for (String tag: on) {
            tb.add(tag, visitor);
        }

        Saxeed saxeed = new Saxeed().setInputString(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        saxeed.addTransformation(tb, baos);
        saxeed.transform();

        return baos.toString();
    }
}
