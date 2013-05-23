package com.marekmaj.hfplatform.service.impl;

import com.marekmaj.hfplatform.event.incoming.Result;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.service.model.InsufficientFundsException;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.NotThreadSafe;
import scala.concurrent.stm.japi.STM;

@NotThreadSafe
public class AkkaStmPublishingAccountService implements AccountService {

    final ResultEventPublisher resultEventPublisher;

    public AkkaStmPublishingAccountService(final ResultEventPublisher resultEventPublisher) {
        this.resultEventPublisher = resultEventPublisher;
    }

    @Override
    public double checkBalance(Account account) {
        final ResultEventPublisher publisher = this.resultEventPublisher;
        final double balance = account.getBalance();
        final ResultEvent resultEvent = publisher.getNextResultEvent();
        resultEvent.setResult(new Result(true, balance));
        publisher.publishEvent(resultEvent);
        return balance;
    }

    @Override
    public boolean transfer(final Account from, final Account to, final double amount) {
        if (from == to){
            Stats.increaseCanceled();
            final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();
            resultEvent.setResult(new Result(false, Double.NaN));
            resultEventPublisher.publishEvent(resultEvent);
            return false;
        }

        try {
            STM.atomic(new Runnable() {
                @Override
                public void run() {
                    from.decreaseBalance(amount);
                    to.increaseBalance(amount);

                    final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();

                    STM.afterRollback(new RollbackAction(resultEventPublisher, resultEvent));

                    STM.afterCommit(new CommitAction(resultEventPublisher, resultEvent, amount));
                }
            });

            return true;
        } catch (InsufficientFundsException e){
            Stats.increaseInsufficient();
            final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();
            resultEvent.setResult(new Result(false, Double.NaN));
            resultEventPublisher.publishEvent(resultEvent);
        }

        return false;
    }
}


class RollbackAction implements Runnable{

    final ResultEventPublisher resultEventPublisher;
    final ResultEvent resultEvent;

    RollbackAction(ResultEventPublisher resultEventPublisher, final ResultEvent resultEvent) {
        this.resultEventPublisher = resultEventPublisher;
        this.resultEvent = resultEvent;
    }

    @Override
    public void run() {
        resultEvent.setIgnoreAttempt(true);
        resultEventPublisher.publishEvent(resultEvent);
        Stats.increaseRollbacks();
    }
}

class CommitAction implements Runnable{

    final ResultEventPublisher resultEventPublisher;
    final ResultEvent resultEvent;
    final Double amount;

    CommitAction(ResultEventPublisher resultEventPublisher, final ResultEvent resultEvent, final  Double amount) {
        this.resultEventPublisher = resultEventPublisher;
        this.resultEvent = resultEvent;
        this.amount = amount;
    }

    @Override
    public void run() {
        resultEvent.setResult(new Result(true, amount));
        resultEventPublisher.publishEvent(resultEvent);
        Stats.increaseCommits();
    }
}