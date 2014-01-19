package com.marekmaj.hfplatform.event.outcoming;


import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.event.incoming.Result;
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

    public void getNextResultEventAndPublish(final Result result, final int id) {
        final ResultEvent resultEvent = getNextResultEvent();
        resultEvent.setId(id);
        resultEvent.setResult(result);
        publishEvent(resultEvent);
    }
}
