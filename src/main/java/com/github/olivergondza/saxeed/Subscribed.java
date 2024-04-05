package com.github.olivergondza.saxeed;

/**
 * Criteria for Visitor-to-tags subscription.
 */
public abstract class Subscribed {

    private static final Subscribed ALL = new Subscribed() {
        @Override
        public boolean isSubscribed(String tagName) {
            return true;
        }
    };

    private static final Subscribed NONE = new Subscribed() {
        @Override
        public boolean isSubscribed(String tagName) {
            return false;
        }
    };

    public static Subscribed toAll() {
        return ALL;
    }

    public static Subscribed to(String... tagNames) {
        return tagNames.length == 0
                ? NONE
                : new Tags(tagNames)
        ;
    }

    public abstract boolean isSubscribed(String tagName);

    private static final class Tags extends Subscribed {

        private final String[] tagNames;

        public Tags(String... tagNames) {
            this.tagNames = tagNames;
            for (String name: tagNames) {
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Empty tag name specified for subscription");
                }
            }
        }

        @Override
        public boolean isSubscribed(String tagName) {
            for (String name: tagNames) {
                if (name.equals(tagName)) return true;
            }

            return false;
        }
    }
}
