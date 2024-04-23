package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import com.github.olivergondza.saxeed.internal.CharChunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddTextTest {
    @Test
    void inside() {
        UpdatingVisitor elementAdder = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                tag.addText("Beginning");
                tag.addChild("b");
            }

            @Override
            public void endTag(Tag.End tag) throws FailedTransforming {
                tag.addChild("e");
                tag.addText("End");
            }
        };

        assertEquals(
                "<r>Beginning<b></b><n></n><e></e>End</r>",
                Util.transform("<r><n/></r>", elementAdder, "r")
        );
    }

    @Test
    void nested() {
        UpdatingVisitor elementAdder = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                Tag.Start ch = tag.addChild("s");
                ch.addText("Nested");
                ch.addChild("n").addText("este");
                ch.addText("d");
            }

            @Override
            public void endTag(Tag.End tag) throws FailedTransforming {
                tag.addChild("e").addChild("e").addText("N");
            }
        };

        assertEquals(
                "<r><s>Nested<n>este</n>d</s><e><e>N</e></e></r>",
                Util.transform("<r></r>", elementAdder, "r")
        );
    }

    @Test
    void charSplitting() {
        UpdatingVisitor bold = new UpdatingVisitor() {

            @Override
            public void chars(Tag.Chars tag, CharChunk chars) {
                String text = chars.get();
                chars.clear();

                for (String chunk : text.split(" ")) {
                    tag.addChild("b").addText(chunk);
                    tag.addText("-");
                }
            }
        };

        assertEquals(
                "<r><s></s><b>replace</b>-<b>with</b>-<b>text</b>-<b>and</b>-<b>elements</b>-<e></e></r>",
                Util.transform("<r><s/>replace with text and elements<e/></r>", bold, "r")
        );
    }
}
