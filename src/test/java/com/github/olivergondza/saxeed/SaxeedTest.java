package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class SaxeedTest {

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
                .add("nosuchtagused", new DeleteAll())
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
        assertEquals(same, Util.transform(
                same,
                (ts) -> {},
                "a"
        ));
    }

    @Test
    void entity() {
        String same = "<r>&lt;<i>&amp;</i>&gt;</r>";
        assertEquals(same, Util.transform(
                same,
                (ts) -> {},
                "r"
        ));
    }

    @Test
    void whitespace() {
        String same = "<r><s> </s> <t>\t</t>\t<nl>\n</nl>\n</r>";
        assertEquals(same, Util.transform(
                same,
                (ts) -> {},
                "r"
        ));
    }

    @Test
    void cdata() {
        assertEquals("<r>Some\tText&amp;Here</r>", Util.transform(
                "<r><![CDATA[Some\tText&Here]]></r>",
                (ts) -> {},
                "r"
        ));
    }

    @Test
    void skip() {
        assertEquals("", Util.transform(
                "<root>skip</root>",
                Tag.Start::skip,
                "root"
        ));

        assertEquals("<a>keepkeep</a>", Util.transform(
                "<a>keep<skip>skip<child/>skip<another>skip</another>skip</skip>keep</a>",
                Tag.Start::skip,
                "skip"
        ));

        assertEquals("<a><w></w></a>", Util.transform(
                "<a><skip><child/></skip><w><skip/></w></a>",
                Tag.Start::skip,
                "skip"
        ));
    }

    @Test
    void empty() {
        String actual = Util.transform(
                "<a>keep1<empty><child/>skip<another>skip</another></empty>keep<s>keep</s></a>",
                Tag.Start::empty,
                "empty"
        );

        assertEquals("<a>keep1<empty></empty>keep<s>keep</s></a>", actual);
    }

    @Test
    void unwrap() {
        String actual = Util.transform(
                "<a>keep<unwrap>keep<child/><another><unwrap><foo/>keep</unwrap></another></unwrap>keep</a>",
                Tag.Start::unwrap,
                "unwrap"
        );

        assertEquals("<a>keepkeep<child></child><another><foo></foo>keep</another>keep</a>", actual);
    }

    @Test
    void attributes() {
        String actual = Util.transform(
                "<root a='v'><child a='v'></child></root>",
                ts -> {
                    ts.removeAttribute("no_such_attr");
                    ts.removeAttributes(List.of("a", "nope"));
                    ts.getAttributes().put("n", "New!");

                },
                "root"
        );

        assertEquals("<root n=\"New!\"><child a=\"v\"></child></root>", actual);
    }

    @Test
    void wrap() {
        Consumer<Tag.Start> wrap = ts -> {
            Tag.Start element = ts.wrapWith("wrapper");
            Map<String, String> attrs = element.getAttributes();
            attrs.put("a1", "v1");
            attrs.put("a2", "v2");
        };
        assertEquals(
                "<wrapper a1=\"v1\" a2=\"v2\"><a></a></wrapper>",
                Util.transform("<a></a>", wrap, "a")
        );

        assertEquals(
                "<r><wrapper a1=\"v1\" a2=\"v2\"><a></a></wrapper></r>",
                Util.transform("<r><a></a></r>", wrap, "a")
        );

        assertEquals(
                "<wrapper a1=\"v1\" a2=\"v2\"><a><i><wrapper a1=\"v1\" a2=\"v2\"><a></a></wrapper></i></a></wrapper>",
                Util.transform("<a><i><a/></i></a>", wrap, "a")
        );
    }

    @Test
    void wrapInvisibleForOtherVisitors() {
        TransformationBuilder t = new TransformationBuilder();

        ArrayList<String> events = new ArrayList<>(3);

        t.add("e", new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                tag.wrapWith("w");
                events.add("wrapped element's visitors");
            }
        });
        t.add("e", new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                assertNotNull(tag.getParent("r"));
                events.add("have same parent in startTag");
            }

            @Override
            public void endTag(Tag.End tag) throws FailedTransforming {
                assertNotNull(tag.getParent("w"));
                events.add("and substituted in enfTag");
            }
        });
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Saxeed().setInputString("<r><e/></r>").addTransformation(t, baos).transform();

        assertEquals("<r><w><e></e></w></r>", baos.toString());
        assertEquals(List.of(
                "wrapped element's visitors", "have same parent in startTag", "and substituted in enfTag"
        ), events);
    }

    @Test
    void addChildren() {
        // Add children to the beginning and to the end of the tag
        UpdatingVisitor addch = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                tag.addChild("head");
                tag.addChild("neck").getAttributes().put("a", "");
            }

            @Override
            public void endTag(Tag.End tag) throws FailedTransforming {
                tag.addChild("tail").addChild("tail");
            }
        };

        assertEquals(
                "<r><head></head><neck a=\"\"></neck><existing></existing><tail><tail></tail></tail></r>",
                Util.transform("<r><existing/></r>", addch, "r")
        );
    }

    @Test
    void illegalUse() {

        try {
            new Saxeed().transform();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("No input data configured", e.getMessage());
        }

        try {
            new Saxeed().setInputString("<a/>").transform();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("No transformations configured", e.getMessage());
        }
    }

    @Test
    void recursiveGeneration() {
        // Generate tags with name the visitor is subscribed to leading to infinite recursion
        UpdatingVisitor uv = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                tag.addChild( "r");
            }
        };

        try {
            Util.transform("<r/>", uv, "r");
            fail();
        } catch (AssertionError ex) {
            assertEquals(
                    "Too deap of a parent loop: r>r>r>r>",
                    ex.getMessage().substring(0, 35)
            );
        }
    }
}
