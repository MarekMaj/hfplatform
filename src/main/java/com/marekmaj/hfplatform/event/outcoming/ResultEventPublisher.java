package com.marekmaj.hfplatform.event.outcoming;


import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public final class ResultEventPublisher {

    private final RingBuffer<ResultEvent> ringBuffer;
    public boolean published = true;
    //private ResultEvent resultEvent;

    public ResultEventPublisher(RingBuffer<ResultEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public ResultEvent getNextResultEvent(){
/*        if (!published){
            System.out.println("NOT PUBLISHED ");
        }*/
        final long currentSequenceNumber = ringBuffer.next();
        ResultEvent resultEvent = ringBuffer.get(currentSequenceNumber);
        resultEvent.setTransactionAttemptNumber(currentSequenceNumber);
        //System.out.println("getting " + currentSequenceNumber);
        //published = false;
        //this.resultEvent = resultEvent;
        return resultEvent;
    }

    public void publishEvent(final ResultEvent resultEvent){
        Stats.increaseReadyToPublishResults();
        ringBuffer.publish(resultEvent.getTransactionAttemptNumber());
        Stats.increasePublishedResults();
/*        if (this.resultEvent != resultEvent ){
            System.out.println("Expected to be published " + this.resultEvent);
        }*/
        //published = true;
    }

    public void getNextResultEventAndPublishSuccess(final int id, final double amount) {
        final ResultEvent resultEvent = getNextResultEvent();
        resultEvent.getResult().setId(id);
        resultEvent.getResult().setAmount(amount);
        resultEvent.getResult().setStatus(true);
        publishEvent(resultEvent);
    }

    public void getNextResultEventAndPublishFailed(final int id) {
        final ResultEvent resultEvent = getNextResultEvent();
        resultEvent.getResult().setId(id);
        resultEvent.getResult().setAmount(Double.NaN);
        resultEvent.getResult().setStatus(false);
        publishEvent(resultEvent);
    }
}
