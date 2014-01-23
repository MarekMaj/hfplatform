package com.marekmaj.hfplatform.utils;


import java.util.concurrent.atomic.AtomicLong;

public final class Stats {

    public static long[] latencies;
    private static boolean gatherStats = true;

    private static final AtomicLong transactionRollbacks = new AtomicLong(0);
    private static final AtomicLong canceled = new AtomicLong(0);
    private static final AtomicLong insufficient = new AtomicLong(0);
    private static final AtomicLong commits = new AtomicLong(0);
    private static final AtomicLong accountRollbacks = new AtomicLong(0);
    private static final AtomicLong publishedResults = new AtomicLong(0);
    private static final AtomicLong readyToPublishResults = new AtomicLong(0);
    private static final AtomicLong loggedResults = new AtomicLong(0L);
    private static final AtomicLong ignoredResults = new AtomicLong(0L);

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

    public static void increaseReadyToPublishResults(){
        if (gatherStats) readyToPublishResults.incrementAndGet();
    }

    public static void increasePublishedResults(){
        if (gatherStats) publishedResults.incrementAndGet();
    }

    public static void increaseLoggedResults(){
        if (gatherStats) loggedResults.incrementAndGet();
    }

    public static void increaseIgnoredResults(){
        if (gatherStats) ignoredResults.incrementAndGet();
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

    public static long getReadyToPublishResults() {
        return readyToPublishResults.get();
    }

    public static long getPublishedResults() {
        return publishedResults.get();
    }

    public static long getLoggedResults() {
        return loggedResults.get();
    }

    public static long getIgnoredResults() {
        return ignoredResults.get();
    }
}
