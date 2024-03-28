package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.List;
import java.util.Map;

public interface UpdatingVisitor {

    default void startTag(Tag.Start tag) throws FailedTransforming {
    }

    default void endTag(Tag.End tag) throws FailedTransforming {
    }

    default void endDocument() throws FailedTransforming {
    }

    static Element newElement(String name, Map<String, String> attrs) {
        Element element = DocumentHelper.createElement(name);
        for (Map.Entry<String, String> a : attrs.entrySet()) {
            element.addAttribute(a.getKey(), a.getValue());
        }
        return element;
    }

    static Element newElement(String name, List<Element> children) {
        Element element = DocumentHelper.createElement(name);
        for (Element child : children) {
            element.add(child);
        }
        return element;
    }

    static Element newElement(String name) {
        return DocumentHelper.createElement(name);
    }
}
