package com.github.olivergondza.saxeed.internal;

/*package*/ enum TagWriteMode {
    /**
     * Keep the tag and its children.
     */
    WRITE(true, /*WRITE*/ null, true),

    /**
     * Remove the tag but keep its content - "unwrap" it from current tag
     */
    UNWRAP(false, WRITE, true),

    /**
     * Remove the tag with all it contains.
     */
    SKIP(false, /*SKIP*/ null, false),

    /**
     * Remove all content of the element but keep the element itself -- "empty" it.
     */
    EMPTY(true, SKIP, false);

    /**
     * Should the node itself be writen;
     */
    /*package*/ final boolean writeMe;

    /**
     * What WriteMode will the children inherit;
     */
    /*package*/ final TagWriteMode children;

    /**
     * Write the text in the current node.
     */
    /*package*/ final boolean writeText;

    TagWriteMode(boolean writeMe, TagWriteMode children, boolean writeText) {
        this.writeMe = writeMe;
        this.children = children == null ? this : children;
        this.writeText = writeText;
    }
}
