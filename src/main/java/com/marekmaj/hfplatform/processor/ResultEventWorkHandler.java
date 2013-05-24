package com.marekmaj.hfplatform.processor;

import com.lmax.disruptor.WorkHandler;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.utils.Stats;


public class ResultEventWorkHandler implements WorkHandler<ResultEvent> {

    private long committed;

    @Override
    public void onEvent(final ResultEvent event) throws Exception {
        if (!event.isIgnoreAttempt()){
            committed++;
            Stats.increaseLoggedResults();

            // TODO chronicle event
        } else {
            Stats.increaseIgnoredResults();
        }

        //System.out.println("committed: " + committed + " logged: " + Stats.getLoggedResults() );
    }
}
