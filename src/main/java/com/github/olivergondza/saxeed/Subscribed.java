package com.github.olivergondza.saxeed;

import java.util.Arrays;
import java.util.List;

/**
 * Criteria for Visitor-to-tags subscription.
 */
@FunctionalInterface
public interface Subscribed {

    /**
     * Subscribe to all tags in the document.
     */
    static Subscribed toAll() {
        return Builder.ALL;
    }

    /**
     * Build subscription criteria.
     */
    static Subscribed.Builder to() {
        return new Subscribed.Builder();
    }

    boolean isSubscribed(TagName tagName);

    final class Builder {
        private static final Subscribed ALL = tagName -> true;

        private Subscribed nsFilter = ALL;
        private Subscribed tagFilter = ALL;

        Builder() {
        }

        /**
         * Match tags regardless of namespace status.
         */
        public Builder anyNamespace() {
            nsFilter = ALL;
            return this;
        }

        /**
         * Match tags in default, not overridden namespace.
         */
        public Builder noNamespace() {
            nsFilter = name -> name.getNsUri().isEmpty();
            return this;
        }

        /**
         * Match tags in default namespace, named or not.
         */
        public Builder defaultNamespace() {
            nsFilter = name -> name.getNsPrefix().isEmpty();
            return this;
        }

        /**
         * Match tags in namespace its uri is in the arguments.
         */
        public Builder namespaceUris(String... uris) {
            List<String> namespaces = list("namespace", uris);
            nsFilter = name -> namespaces.contains(name.getNsUri());
            return this;
        }

        /**
         * Match any local tag name.
         */
        public Builder anyTag() {
            tagFilter = ALL;
            return this;
        }

        /**
         * Match local tag names specified in arguments.
         */
        public Builder tagNames(String... locals) {
            List<String> tags = list("tag", locals);
            tagFilter = name -> tags.contains(name.getLocal());
            return this;
        }

        public Subscribed build() {
            assert nsFilter != null;
            assert tagFilter != null;

            return name -> tagFilter.isSubscribed(name) && nsFilter.isSubscribed(name);
        }

        private static List<String> list(String type, String[] vals) {
            if (vals.length == 0) {
                throw new IllegalArgumentException("Subscribing to 0 " + type + "s means no subscription at all");
            }

            for (String name: vals) {
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Empty " + type + " name specified for subscription in: " + Arrays.toString(vals));
                }
            }
            return List.of(vals);
        }
    }
}
