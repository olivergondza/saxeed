package com.github.olivergondza.saxeed;

import java.util.Objects;

/**
 * Namespace aware tag name.
 */
public class TagName {

    private final String local;
    private final String qName;

    private final String uri;
    private final String prefix;

    public static TagName fromSaxArgs(String uri, String localName, String qName) {
        boolean noNsInTagName = Objects.equals(localName, qName);
        if (uri.isEmpty()) {
            assert noNsInTagName;
            return new TagName("", "", localName);
        }

        assert qName.endsWith(localName): String.format("Tag name ('%s') does not start with local name ('%s')", qName, localName);

        if (noNsInTagName) {
            return new TagName(uri, "", localName);
        }

        return new TagName(uri, qName.replaceFirst(":.*", ""), localName);
    }

    public static TagName noNs(String local) {
        return new TagName("", "", local);
    }

    public static TagName withNs(String uri, String local) {
        return new TagName(uri, "", local);
    }

    public static TagName withNs(String uri, String prefix, String local) {
        return new TagName(uri, prefix, local);
    }

    /**
     * Create tag in specified namespace.
     */
    public TagName(String uri, String prefix, String local) {
        this.uri = Objects.requireNonNull(uri);
        this.prefix = Objects.requireNonNull(prefix);
        this.local = Objects.requireNonNull(local);

        if (local.isEmpty()) throw new IllegalArgumentException("Tag cannot have local name an empty string");

        if (uri.isEmpty() && !prefix.isEmpty()) throw new IllegalArgumentException("Tag cannot have NS name, but no NS URI");

        qName = prefix.isEmpty()
                ? local
                : prefix + ":" + local
        ;
    }

    @Override
    public String toString() {
        return String.format("TagName{local='%s', uri='%s', ns='%s'}", local, uri, prefix);
    }

    public String getNsUri() {
        return uri;
    }

    public String getNsPrefix() {
        return prefix;
    }

    public String getLocal() {
        return local;
    }

    public String getQualifiedName() {
        return qName;
    }

    public TagName inheritNamespace(String name) {
        return new TagName(uri, prefix, name);
    }
}
