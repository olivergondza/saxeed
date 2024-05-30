package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscribeTest {
    @Test
    void builder() {
        TagName localTag = TagName.noNs("local");
        TagName defNsTag = TagName.withNs("uri", "local");
        TagName namedNsTag = TagName.withNs("uri", "prefix", "local");

        Subscribed everything = Subscribed.to().build();
        assertTrue(everything.isSubscribed(localTag));
        assertTrue(everything.isSubscribed(defNsTag));
        assertTrue(everything.isSubscribed(namedNsTag));

        Subscribed noNs = Subscribed.to().noNamespace().build();
        assertTrue(noNs.isSubscribed(localTag));
        assertFalse(noNs.isSubscribed(defNsTag));
        assertFalse(noNs.isSubscribed(namedNsTag));

        Subscribed defNs = Subscribed.to().defaultNamespace().build();
        assertTrue(defNs.isSubscribed(localTag));
        assertTrue(defNs.isSubscribed(defNsTag));
        assertFalse(defNs.isSubscribed(namedNsTag));

        Subscribed namedNs = Subscribed.to().namespaceUris("uri").build();
        assertFalse(namedNs.isSubscribed(localTag));
        assertTrue(namedNs.isSubscribed(defNsTag));
        assertTrue(namedNs.isSubscribed(namedNsTag));

        Subscribed tags = Subscribed.to().tagNames("local").build();
        assertTrue(tags.isSubscribed(localTag));
        assertTrue(tags.isSubscribed(defNsTag));
        assertTrue(tags.isSubscribed(namedNsTag));

        Subscribed tagsNoMatch = Subscribed.to().tagNames("noSuchTag").build();
        assertFalse(tagsNoMatch.isSubscribed(localTag));
        assertFalse(tagsNoMatch.isSubscribed(defNsTag));
        assertFalse(tagsNoMatch.isSubscribed(namedNsTag));

        Subscribed combined = Subscribed.to().tagNames("local").namespaceUris("uri").build();
        assertFalse(combined.isSubscribed(localTag));
        assertTrue(combined.isSubscribed(defNsTag));
        assertTrue(combined.isSubscribed(namedNsTag));

        Subscribed overriden = Subscribed.to().tagNames("nope").noNamespace().anyTag().anyNamespace().build();
        assertTrue(overriden.isSubscribed(localTag));
        assertTrue(overriden.isSubscribed(defNsTag));
        assertTrue(overriden.isSubscribed(namedNsTag));
    }

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

                        tag.addChild("as");
                    }

                    @Override
                    public void endTag(Tag.End tag) throws FailedTransforming {
                        if (tag.isGenerated()) return;

                        tag.addChild("ae");
                    }
                }))
                .add("r", new UpdatingVisitor() {
                    @Override
                    public void startTag(Tag.Start tag) throws FailedTransforming {
                        tag.addChild("ns");
                    }

                    @Override
                    public void endTag(Tag.End tag) throws FailedTransforming {
                        tag.addChild("ne");
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
