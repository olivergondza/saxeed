package com.github.olivergondza.saxeed;

/*package*/ enum TagWriteMode {
    /**
     * Keep the tag and its children.
     */
    WRITE(true, /*WRITE*/ null),

    /**
     * Remove the tag but keep its content - "unwrap" it from current tag
     */
    UNWRAP(false, WRITE),

    /**
     * Remove the tag with all it contains.
     */
    SKIP(false, /*SKIP*/ null),

    /**
     * Remove all content of the element but keep the element itself -- "empty" it.
     */
    EMPTY(true, SKIP);

    /*package*/ final boolean writeMe;
    /*package*/ final TagWriteMode children;

    TagWriteMode(boolean writeMe, TagWriteMode children) {
        this.writeMe = writeMe;
        this.children = children == null ? this : children;
    }
}
