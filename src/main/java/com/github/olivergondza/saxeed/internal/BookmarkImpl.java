package com.github.olivergondza.saxeed.internal;

import com.github.olivergondza.saxeed.Bookmark;
import com.github.olivergondza.saxeed.TagName;

import java.util.Objects;

public class BookmarkImpl implements Bookmark {
    private String value;
    private boolean omitted = false;

    static BookmarkImpl from(BookmarkImpl parent, TagName name, int count) {
        return new BookmarkImpl(pathFrom(parent, name, count));
    }

    static String pathFrom(BookmarkImpl parent, TagName name, int count) {
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            assert parent.value != null: "Parent tag " + parent + " must not be written by the time its children " + name + " are still being created.";
            sb.append(parent.value);
        }

        sb.append('/').append(name.getLocal());
        String nsUri = name.getNsUri();
        if (!nsUri.isEmpty()) {
            sb.append('<').append(nsUri).append('>');
        }
        sb.append('[').append(count).append(']');
        return sb.toString();
    }

    private BookmarkImpl(String bookmark) {
        this.value = bookmark;
    }

    /*package*/ void update(String value) {
        this.value = value;
    }

    /*package*/ void omit() {
        omitted = true;
    }

    @Override
    public boolean isOmitted() {
        return omitted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookmarkImpl bookmark = (BookmarkImpl) o;

        // If either is omitted, they are not equal. This is to prevent that omitted bookmark would match real tag based
        // on value clash.
        if (omitted || bookmark.omitted) {
            return false;
        }

        return Objects.equals(value, bookmark.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, omitted);
    }

    @Override
    public String toString() {
        return value;
    }
}
