package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.internal.TransformationHandler;

import javax.xml.stream.XMLStreamWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class TransformationBuilder {
    private final LinkedHashMap<UpdatingVisitor, Subscribed> visitors = new LinkedHashMap<>();

    public TransformationBuilder() {
    }

    public TransformationBuilder add(String tagName, List<UpdatingVisitor> visitors) {
        return add(Subscribed.to(tagName), visitors);
    }

    public TransformationBuilder add(String tagName, UpdatingVisitor visitor) {
        return add(Subscribed.to(tagName), visitor);
    }

    public TransformationBuilder add(Subscribed subs, UpdatingVisitor visitor) {
        if (visitors.containsKey(visitor)) {
            throw new IllegalStateException("Repeated addition of visitor " + visitor);
        }

        visitors.put(visitor, subs);
        return this;
    }

    public TransformationBuilder add(Subscribed subs, Collection<UpdatingVisitor> visitors) {
        for (UpdatingVisitor visitor : visitors) {
            add(subs, visitor);
        }
        return this;
    }

    public TransformationHandler build(XMLStreamWriter writer, AutoCloseable closeAction) {
        return new TransformationHandler(visitors, writer, closeAction);
    }
}
