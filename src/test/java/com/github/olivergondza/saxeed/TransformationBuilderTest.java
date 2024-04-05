package com.github.olivergondza.saxeed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformationBuilderTest {

    @Test
    void repeat() {
        UpdatingVisitor v = new UpdatingVisitor() {};

        try {
            new TransformationBuilder().add("a", v).add("b", v);
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }
    }
}
