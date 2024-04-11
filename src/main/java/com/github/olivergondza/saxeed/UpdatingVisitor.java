package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;

/**
 * Visitor listening and modifying resulting stream.
 */
public interface UpdatingVisitor {

    default void startDocument() throws FailedTransforming {
    }

    default void startTag(Tag.Start tag) throws FailedTransforming {
    }

    default void endTag(Tag.End tag) throws FailedTransforming {
    }

    default void endDocument() throws FailedTransforming {
    }
}
