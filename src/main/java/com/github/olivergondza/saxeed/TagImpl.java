package com.github.olivergondza.saxeed;

import org.dom4j.Element;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Element representation for the purposes of visiting.
 *
 * It provides accessors to element data, and mutator methods for a visitor to communicate the decision of what to write.
 *
 * Implementation note: The actual class is hidden for client to only use the interface(s). This has dedicated
 * sub-interfaces for Visitor call argument, but the exact same instance is passed. The point is to provide a compile-time
 * guarantee that methods that are called as certain stage can legally be called. IOW, when closing tag, method modifying
 * attributes will nt be available.
 */
/*package*/ class TagImpl implements
        Tag, Tag.Start, Tag.End
{

    private /*almost final*/ TagImpl parent;

    private final String name;
    private final Attributes attrs;

    /**
     * The element is not part of the input stream, but it has been generated by a visitor.
     */
    private final boolean generated;

    /**
     * Possibly modified list of attributes.
     */
    private Map<String, String> attributes;

    /**
     * Modifiable indication of tag write/delete.
     */
    private TagWriteMode writeMode = TagWriteMode.WRITE;

    /**
     * List of children to be added.
     */
    private final List<Element> childrenToAdd = new ArrayList<>();

    /**
     *  Element that current element should be surrounded with.
     */
    private Element wrapWith;

    /*package*/ TagImpl(TagImpl parent, String name, Attributes attrs, boolean generated) {
        this.parent = parent;
        this.name = name;
        this.attrs = attrs;
        this.generated = generated;

        // Inherit the write mode based on the parent's one.
        if (parent != null) {
            writeMode = parent.writeMode.children;
        }
    }

    @Override
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>(attrs.getLength());
            for (int i = 0; i < attrs.getLength(); i++) {
                attributes.put(attrs.getQName(i), attrs.getValue(i));
            }
        }

        return attributes;
    }

    @Override
    public boolean isNamed(String name) {
        return Objects.equals(name, this.name);
    }

    @Override
    public boolean isGenerated() {
        return generated;
    }

    // This returns the most restricted interface as the parent have always been written.
    @Override
    public Tag getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Tag getParent(String name) {
        return parent != null && parent.isNamed(name) ? parent : null;
    }

    @Override
    public Tag getAncestor(String name) {
        for (TagImpl tag = this.parent; tag != null; tag = tag.parent){
            if (tag.isNamed(name)) return tag;
        }
        return null;
    }

    @Override
    public void addChildren(List<Element> children) {
        childrenToAdd.addAll(children);
    }

    @Override
    public void addChild(Element child) {
        childrenToAdd.add(child);
    }

    /**
     * Decide if this element should be written or not.
     */
    @Override
    public boolean isOmitted() {
        return !writeMode.writeMe;
    }

    @Override
    public void wrapWith(Element wrapWith) {
        if (this.wrapWith != null) throw new AssertionError(
                "Unable to wrap with multiple elements. Existing " + wrapWith
        );

        this.wrapWith = wrapWith;
    }

    @Override
    public void skip() {
        writeMode = TagWriteMode.SKIP;
    }

    @Override
    public void unwrap() {
        writeMode = TagWriteMode.UNWRAP;
    }

    @Override
    public void empty() {
        writeMode = TagWriteMode.EMPTY;
    }

    @Override
    public boolean removeAttributes(Collection<String> attrs) {
        return getAttributes().keySet().removeAll(attrs);
    }

    @Override
    public String removeAttribute(String attr) {
        return getAttributes().remove(attr);
    }

    /*package*/ List<Element> getTagsAdded() {
        return childrenToAdd;
    }

    /*package*/ Element getWrapWith() {
        return wrapWith;
    }

    // The hierarchy of tag parents needs to be fixed as we have injected a new one
    // between `this` and `this.parent`
    /*package*/ TagImpl wrapInto(String name, Attributes attrs) {
        setParent(new TagImpl(this.parent, name, attrs, false));
        return this.parent;
    }

    private void setParent(TagImpl parent) {
        if (this.parent != null) {

            if (parent.parent != this.parent.parent) throw new AssertionError(
                    "Unable to set parent, invalid grandparent"
            );

            ArrayList<TagImpl> parents = new ArrayList<>();
            for (TagImpl tag = this; tag != null; tag = tag.parent) {
                parents.add(tag);
                if (tag == parent) throw new AssertionError(
                        String.format("Illegal parent insert of %s into %s", parent.getName(), this)
                );
            }

            if (parents.size() != new HashSet<>(parents).size()) {
                throw new AssertionError("Duplicates detected in parent chain");
            }
        }

        this.parent = parent;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int limit = 100;
        for (Tag tag = this; tag != null; tag = getParent()) {
            if (--limit < 0) {
                throw new AssertionError("Too deap of a parent loop: " + sb);
            }
            sb.append("/").append(tag.getName());
            if (isOmitted()) {
                sb.append(";omitted");
            }
            if (isGenerated()) {
                sb.append(";generated");
            }
        }
        return sb.toString();
    }
}
