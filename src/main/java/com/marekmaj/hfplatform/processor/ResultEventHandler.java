package com.marekmaj.hfplatform.processor;


import com.lmax.disruptor.EventHandler;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.utils.Stats;

import java.util.concurrent.CountDownLatch;

public class ResultEventHandler implements EventHandler<ResultEvent> {

    private long count;
    private CountDownLatch latch;
    private long localSequence = -1;

    public void reset(final CountDownLatch latch, final long expectedCount) {
        this.latch = latch;
        count = expectedCount;
    }

    @Override
    public void onEvent(final ResultEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        if (!event.isIgnoreAttempt()){
            // increase just for check
            Stats.increaseLoggedResults();

            // TODO chronicle event
        } else {
            Stats.increaseIgnoredResults();
        }

        if (localSequence + 1 == sequence) {
            localSequence = sequence;
        }
        else {
            System.err.println("Expected: " + (localSequence + 1) + "found: " + sequence);
        }

        System.err.println("sequence: " + sequence + " logged: " + Stats.getLoggedResults().get() );
        if (count == (Stats.getLoggedResults().get() -1L)) {
            latch.countDown();
        }
    }
}