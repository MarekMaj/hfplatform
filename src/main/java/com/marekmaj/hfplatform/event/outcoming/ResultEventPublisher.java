package com.marekmaj.hfplatform.event.outcoming;


import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public final class ResultEventPublisher {

    private final RingBuffer<ResultEvent> ringBuffer;
    private long currentSequenceNumber = -1;

    public ResultEventPublisher(RingBuffer<ResultEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public ResultEvent getNextResultEvent(){
        currentSequenceNumber = ringBuffer.next();
        ResultEvent resultEvent = ringBuffer.get(currentSequenceNumber);
        resultEvent.setIgnoreAttempt(false);
        return resultEvent;
    }

    public void publishEvent(){
        Stats.increaseReadyToPublishResults();
        ringBuffer.publish(currentSequenceNumber);
        Stats.increasePublishedResults();
    }

    public void getNextResultEventAndPublishSuccess(final int id, final double amount) {
        final ResultEvent resultEvent = getNextResultEvent();
        resultEvent.getResult().setId(id);
        resultEvent.getResult().setAmount(amount);
        resultEvent.getResult().setStatus(true);
        publishEvent();
    }

    public void getNextResultEventAndPublishFailed(final int id) {
        final ResultEvent resultEvent = getNextResultEvent();
        resultEvent.getResult().setId(id);
        resultEvent.getResult().setAmount(Double.NaN);
        resultEvent.getResult().setStatus(false);
        publishEvent();
    }
}
