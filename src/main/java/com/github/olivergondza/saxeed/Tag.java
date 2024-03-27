package com.github.olivergondza.saxeed;

import org.dom4j.Element;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * General interface for tag visiting.
 */
public interface Tag {

    boolean isNamed(String name);

    boolean isGenerated();

    Tag getParent();

    String getName();

    Tag getParent(String name);

    Tag getAncestor(String name);

    Map<String, String> getAttributes();

    boolean isOmitted();

    interface Start {
        void wrapWith(Element wrapWith);

        void skip();

        void unwrap();

        void empty();

        boolean removeAttributes(Collection<String> attrs);

        String removeAttribute(String attr);

        void addChildren(List<Element> children);

        void addChild(Element child);
    }

    interface End {
        void addChildren(List<Element> children);

        void addChild(Element child);
    }
}
