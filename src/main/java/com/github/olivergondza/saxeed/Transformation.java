package com.github.olivergondza.saxeed;

public interface Transformation {
    static TransformationBuilder build() {
        return new TransformationBuilder();
    }

    static Transformation fromAnnotations() {
        throw new UnsupportedOperationException();
    }
}
