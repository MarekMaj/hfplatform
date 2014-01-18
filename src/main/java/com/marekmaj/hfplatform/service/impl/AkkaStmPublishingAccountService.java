package com.marekmaj.hfplatform.service.impl;

import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.Result;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
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
    public double checkBalance(BalanceAccountCommand balanceAccountCommand) {
        final ResultEventPublisher publisher = this.resultEventPublisher;
        final double balance = balanceAccountCommand.getAccount().getBalance();
        final ResultEvent resultEvent = publisher.getNextResultEvent();
        resultEvent.setId(balanceAccountCommand.getId());
        resultEvent.setResult(new Result(true, balance));
        publisher.publishEvent(resultEvent);
        return balance;
    }

    @Override
    public boolean transfer(final TransferAccountCommand transferAccountCommand) {
        if (transferAccountCommand.getFrom() == transferAccountCommand.getTo()){
            Stats.increaseCanceled();
            final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();
            resultEvent.setId(transferAccountCommand.getId());
            resultEvent.setResult(new Result(false, Double.NaN));
            resultEventPublisher.publishEvent(resultEvent);
            return false;
        }

        try {
            STM.atomic(new Runnable() {
                @Override
                public void run() {
                    transferAccountCommand.getFrom().decreaseBalance(transferAccountCommand.getAmount());
                    transferAccountCommand.getTo().increaseBalance(transferAccountCommand.getAmount());

                    final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();
                    resultEvent.setId(transferAccountCommand.getId());

                    STM.afterRollback(new RollbackAction(resultEventPublisher, resultEvent));

                    STM.afterCommit(new CommitAction(resultEventPublisher, resultEvent, transferAccountCommand.getAmount()));
                }
            });

            return true;
        } catch (InsufficientFundsException e){
            Stats.increaseInsufficient();
            final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();
            resultEvent.setId(transferAccountCommand.getId());
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