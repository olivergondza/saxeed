package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscribeTest {
    @Test
    void subscription() {
        TransformationBuilder tb = new TransformationBuilder()
                .add(Subscribed.toAll(), List.of(new UpdatingVisitor() {
                    @Override
                    public void startTag(Tag.Start tag) throws FailedTransforming {
                        tag.getAttributes().put("all", "");
                    }
                }))
                .add("nosuchtagininput", List.of(new UpdatingVisitor() {
                    @Override
                    public void startTag(Tag.Start tag) throws FailedTransforming {
                        tag.unwrap();
                    }
                }))
                .add("l", new UpdatingVisitor() {
                    @Override
                    public void startTag(Tag.Start tag) throws FailedTransforming {
                        tag.getAttributes().put("leaf", "");
                    }
                })
        ;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Saxeed()
                .setInputString("<r><n><l/></n></r>")
                .addTransformation(tb, baos)
                .transform();

        assertEquals("<r all=\"\"><n all=\"\"><l all=\"\" leaf=\"\"></l></n></r>", baos.toString());
    }

    @Test
    void reversalForEndTag() {
        TransformationBuilder tb = new TransformationBuilder()
                .add(Subscribed.toAll(), List.of(new UpdatingVisitor() {
                    @Override
                    public void startTag(Tag.Start tag) throws FailedTransforming {
                        if (tag.isGenerated()) return;

                        tag.addChild(newElement("as"));
                    }

                    @Override
                    public void endTag(Tag.End tag) throws FailedTransforming {
                        if (tag.isGenerated()) return;

                        tag.addChild(newElement("ae"));
                    }
                }))
                .add("r", new UpdatingVisitor() {
                    @Override
                    public void startTag(Tag.Start tag) throws FailedTransforming {
                        tag.addChild(newElement("ns"));
                    }

                    @Override
                    public void endTag(Tag.End tag) throws FailedTransforming {
                        tag.addChild(newElement("ne"));
                    }
                })
        ;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Saxeed()
                .setInputString("<r></r>")
                .addTransformation(tb, baos)
                .transform();

        assertEquals("<r><as></as><ns></ns><ne></ne><ae></ae></r>", baos.toString());
    }
}
