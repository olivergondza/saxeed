package com.github.olivergondza.saxeed;

import java.util.Collection;
import java.util.Map;

/**
 * Visited tag.
 *
 * Most generic interface permitting data reading only.
 */
public interface Tag {

    /**
     * Get element name.
     */
    String getName();

    /**
     * Determine if current tag's name is @name.
     */
    boolean isNamed(String name);

    /**
     * Get Tag's parent
     *
     * @return null for root tag, parent otherwise.
     */
    Tag getParent();

    /**
     * Get Tag's parent iff its name is @name;
     *
     * @return null for root tag, or when parent name differs, parent otherwise.
     */
    Tag getParent(String name);

    /**
     * Get the closest ancestor (wrapping tag) its name is @name.
     *
     * @return null if there is no such ancestor, ancestor otherwise.
     */
    Tag getAncestor(String name);

    /**
     * Get modifiable attribute map.
     */
    Map<String, String> getAttributes();

    /**
     * Determine if the current tag was added by a visitor.
     *
     * In other words, it was not part of an input stream.
     *
     * @see com.github.olivergondza.saxeed.Tag.Start#addChild(String)
     * @see com.github.olivergondza.saxeed.Tag.Start#wrapWith(String)
     */
    boolean isGenerated();

    /**
     * Determine if the current was removed.
     *
     * In other words, it will not be written to the Target.
     */
    boolean isOmitted();

    /**
     * Tag during the startTag event.
     *
     * Permits modification possible as the tag was not written yet.
     */
    interface Start extends Tag {

        /**
         * Remove tag and all its content and children.
         */
        void skip();

        /**
         * Remove tag and its content, but keep its child tags.
         */
        void unwrap();

        /**
         * Remove all tags children, but keep the element itself.
         */
        void empty();

        boolean removeAttributes(Collection<String> attrs);

        String removeAttribute(String attr);

        /**
         * Add new child element.
         *
         * Its attributes and children can be added after.
         *
         * @return Tag instance added.
         */
        Tag.Start addChild(String name);

        /**
         * Add new parent element for the current tag.
         *
         * Its attributes and children can be added after.
         *
         * @return Tag instance created.
         */
        Tag.Start wrapWith(String name);

        /**
         * Set text to write after opening tag.
         */
        void addText(String text);
    }

    interface Chars extends Tag {

        /**
         * Add new child element.
         *
         * Its attributes and children can be added after.
         *
         * @return Tag instance added.
         */
        Tag.Start addChild(String name);

        /**
         * Set text to write before closing tag.
         */
        void addText(String text);
    }

    /**
     * Tag during the endTag event.
     *
     * Permits some modifications as the opening tag was already written.
     */
    interface End extends Tag {
        /**
         * Add new child element.
         *
         * Its attributes and children can be added after.
         *
         * @return Tag instance added.
         */
        Tag.Start addChild(String name);

        /**
         * Set text to write before closing tag.
         */
        void addText(String text);
    }
}
