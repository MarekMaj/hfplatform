package com.marekmaj.hfplatform.processor;


import com.lmax.disruptor.EventHandler;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.utils.Stats;

import java.util.concurrent.CountDownLatch;

public class ResultEventHandler implements EventHandler<ResultEvent> {

    private long committed;
    private long ignored;
    private long count;
    private CountDownLatch latch;
    private long localSequence = -1;

    public void reset(final CountDownLatch latch, final long expectedCount) {
        this.latch = latch;
        count = expectedCount - localSequence;
    }

    @Override
    public void onEvent(final ResultEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        if (!event.isIgnoreAttempt()){
            committed++;
            Stats.increaseLoggedResults();

            // TODO chronicle event
        } else {
            ignored++;
            Stats.increaseIgnoredResults();
        }

        if (localSequence + 1 == sequence) {
            localSequence = sequence;
        }
        else {
            System.err.println("Expected: " + (localSequence + 1) + "found: " + sequence);
        }

        //System.out.println("sequence: " + sequence + " logged: " + Stats.getLoggedResults() );
/*        if (endOfBatch){
            System.out.println("batch: " + sequence + " logged: " + Stats.getLoggedResults() );
        }*/
        if (count == committed + ignored) {
            latch.countDown();
        }
    }
}