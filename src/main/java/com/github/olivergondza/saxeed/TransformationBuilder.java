package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.internal.TransformationHandler;

import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformationBuilder {
    private final Map<String, List<UpdatingVisitor>> tagToVisitors = new HashMap<>();

    public TransformationBuilder() {
    }

    public TransformationBuilder put(String tagName, List<UpdatingVisitor> visitors) {
        tagToVisitors.put(tagName, new ArrayList<>(visitors));
        return this;
    }

    public TransformationBuilder put(String tagName, UpdatingVisitor visitor) {
        return put(tagName, List.of(visitor));
    }

    public TransformationBuilder add(String tagName, List<UpdatingVisitor> visitors) {
        if (!tagToVisitors.containsKey(tagName)) {
            put(tagName, visitors);
        } else {
            tagToVisitors.get(tagName).addAll(visitors);
        }
        return this;
    }

    public TransformationBuilder add(String tagName, UpdatingVisitor visitors) {
        if (!tagToVisitors.containsKey(tagName)) {
            put(tagName, visitors);
        } else {
            tagToVisitors.get(tagName).add(visitors);
        }
        return this;
    }

    public TransformationHandler build(XMLStreamWriter writer) {
        return new TransformationHandler(tagToVisitors, writer);
    }
}
