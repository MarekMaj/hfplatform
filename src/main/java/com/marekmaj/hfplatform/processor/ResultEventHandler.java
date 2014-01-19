package com.marekmaj.hfplatform.processor;

import com.lmax.disruptor.EventHandler;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.utils.Stats;
import net.openhft.chronicle.Excerpt;
import net.openhft.chronicle.IndexedChronicle;

import java.util.concurrent.CountDownLatch;

public class ResultEventHandler implements EventHandler<ResultEvent> {

    final IndexedChronicle chronicle;

    private long committed;
    private long ignored;
    private long count;
    private CountDownLatch latch;
    private long localSequence = -1;

    public ResultEventHandler(IndexedChronicle chronicle) {
        this.chronicle = chronicle;
    }

    public void reset(final CountDownLatch latch, final long expectedCount) {
        this.latch = latch;
        count = expectedCount - localSequence;
    }

    @Override
    public void onEvent(final ResultEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        if (!event.isIgnoreAttempt()){
            committed++;
            Stats.increaseLoggedResults();
            //chronicleEvent(event);
            Stats.finishTimes[event.getId()] = System.nanoTime();
        } else {
            ignored++;
            Stats.increaseIgnoredResults();
        }

        // TODO po co to
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
        if (count == committed) {
            latch.countDown();
        }
    }

/*    private void chronicleEvent(ResultEvent event) {
        final Excerpt excerpt = chronicle.createExcerpt();
        excerpt.startExcerpt(8 + 8 + 8);
        excerpt.writeLong(System.nanoTime());
        excerpt.writeLong(event.getTransactionAttemptNumber());
        excerpt.writeDouble(event.getResult().getAmount());
        excerpt.finish();
    }*/
}