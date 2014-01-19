package com.marekmaj.hfplatform.event.outcoming;


import com.lmax.disruptor.EventFactory;

public final class ResultEvent {

    private Result result;
    private boolean ignoreAttempt = false;
    // do not need this in fact, ordering will happen in outputDisruptor:
    private long transactionAttemptNumber;

    public ResultEvent(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public boolean isIgnoreAttempt() {
        return ignoreAttempt;
    }

    public void setIgnoreAttempt(boolean ignoreAttempt) {
        this.ignoreAttempt = ignoreAttempt;
    }

    public long getTransactionAttemptNumber() {
        return transactionAttemptNumber;
    }

    public void setTransactionAttemptNumber(long transactionAttemptNumber) {
        this.transactionAttemptNumber = transactionAttemptNumber;
    }

    public final static EventFactory<ResultEvent> RESULT_EVENT_FACTORY = new EventFactory<ResultEvent>() {
        public ResultEvent newInstance() {
            return new ResultEvent(new Result(-1));
        }
    };
}


