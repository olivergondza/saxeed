package com.github.olivergondza.saxeed;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformationBuilder {
    private final Map<String, List<TransformationHandler.Visitor>> tagToVisitors;

    public TransformationBuilder() {
        this(new HashMap<>());
    }

    public TransformationBuilder(Map<String, List<TransformationHandler.Visitor>> tagToVisitors) {
        this.tagToVisitors = tagToVisitors;
    }

    public TransformationBuilder put(String tagName, List<TransformationHandler.Visitor> visitors) {
        tagToVisitors.put(tagName, visitors);
        return this;
    }

    public TransformationBuilder put(String tagName, TransformationHandler.Visitor visitor) {
        tagToVisitors.put(tagName, new ArrayList<>(List.of(visitor)));
        return this;
    }

    public TransformationBuilder add(String tagName, List<TransformationHandler.Visitor> visitors) {
        if (!tagToVisitors.containsKey(tagName)) {
            tagToVisitors.put(tagName, new ArrayList<>(visitors));
        } else {
            tagToVisitors.get(tagName).addAll(visitors);
        }
        return null;
    }

    public TransformationBuilder add(String tagName, TransformationHandler.Visitor visitors) {
        if (!tagToVisitors.containsKey(tagName)) {
            tagToVisitors.put(tagName, new ArrayList<>(List.of(visitors)));
        } else {
            tagToVisitors.get(tagName).add(visitors);
        }
        return null;
    }

    public TransformationHandler build(XMLStreamWriter writer) {
        return new TransformationHandler(tagToVisitors, writer);
    }
}
