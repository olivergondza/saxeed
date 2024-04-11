package com.github.olivergondza.saxeed;

import java.util.Collection;
import java.util.Map;

/**
 * General interface for tag visiting.
 */
public interface Tag {

    boolean isNamed(String name);

    Tag getParent();

    String getName();

    Tag getParent(String name);

    Tag getAncestor(String name);

    Map<String, String> getAttributes();

    boolean isGenerated();

    boolean isOmitted();

    interface Start extends Tag {

        void skip();

        void unwrap();

        void empty();

        boolean removeAttributes(Collection<String> attrs);

        String removeAttribute(String attr);

        Tag.Start addChild(String name);

        Tag.Start wrapWith(String name);
    }

    interface End extends Tag {
        Tag.Start addChild(String name);
    }
}
