package com.marekmaj.hfplatform.utils;


import java.util.concurrent.atomic.AtomicLong;

public final class Stats {

    private static boolean gatherStats = true;

    private static final AtomicLong transactionRollbacks = new AtomicLong(0);
    private static final AtomicLong canceled = new AtomicLong(0);
    private static final AtomicLong insufficient = new AtomicLong(0);
    private static final AtomicLong commits = new AtomicLong(0);
    private static final AtomicLong accountRollbacks = new AtomicLong(0);

    public static void increaseRollbacks(){
        if (gatherStats) transactionRollbacks.incrementAndGet();
    }

    public static void increaseCanceled(){
        if (gatherStats) canceled.incrementAndGet();
    }

    public static void increaseInsufficient(){
        if (gatherStats) insufficient.incrementAndGet();
    }

    public static void increaseCommits(){
        if (gatherStats) commits.incrementAndGet();
    }

    public static void increaseAccountRollbacks(){
        if (gatherStats) accountRollbacks.incrementAndGet();
    }

    public static long getTransactionRollbacks() {
        return transactionRollbacks.get();
    }

    public static long getCanceled() {
        return canceled.get();
    }

    public static long getInsufficient() {
        return insufficient.get();
    }

    public static long getCommits() {
        return commits.get();
    }

    public static long getAccountRollbacks() {
        return accountRollbacks.get();
    }
}
