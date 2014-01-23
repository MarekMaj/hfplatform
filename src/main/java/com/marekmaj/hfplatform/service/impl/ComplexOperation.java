package com.marekmaj.hfplatform.service.impl;


import java.util.concurrent.ThreadLocalRandom;

public final class ComplexOperation {

    private static final int BASE_OPERATIONS = 10;
    private static final int COMPLEXITY_FACTOR = Integer.getInteger("op.factor", 0);

    private long value = 0;

    public void performComplexOperationInIsolatedManner() {
        if (COMPLEXITY_FACTOR == 0) return;
        for (int i=0; i < BASE_OPERATIONS * COMPLEXITY_FACTOR; i++) {
            value += ThreadLocalRandom.current().nextLong(100);
        }
    }

    public long getValue() {
        return value;
    }
}
