package com.github.olivergondza.saxeed;

/**
 * A reference to a tag acquired in a previous Saxeed run, to be queried in the next run.
 *
 * The reference is valid only between two consecutive runs. Provided the document was modified between the executions,
 * Saxeed provides no guarantee it will match anything, fail predictably, or match the intended tag.
 *
 * To bookmark an element, call {@link Tag#bookmark()}. The object returned is valid outside the Saxeed transformation.
 * In the next processing, use {@link Tag#isBookmarked(Bookmark)} or {@link Tag#isBookmarked(java.util.List)} to
 * identify the bookmarked element.
 */
public interface Bookmark {

    /**
     * Determine if the tag was removed from the output.
     */
    boolean isOmitted();
}
