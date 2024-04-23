package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

public class Util {
    static String transform(String input, Consumer<Tag.Start> lambda, String... on) {
        UpdatingVisitor visitor = new UpdatingVisitor() {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                lambda.accept(tag);
            }
        };

        return transform(input, visitor, on);
    }

    static String transform(String input, UpdatingVisitor visitor, String... on) {
        TransformationBuilder tb = new TransformationBuilder();
        tb.add(Subscribed.to(on), visitor);

        return transform(input, tb);
    }

    static String transform(String input, TransformationBuilder tb) {
        Saxeed saxeed = new Saxeed().setInputString(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        saxeed.addTransformation(tb, baos);
        saxeed.transform();

        return baos.toString();
    }
}
