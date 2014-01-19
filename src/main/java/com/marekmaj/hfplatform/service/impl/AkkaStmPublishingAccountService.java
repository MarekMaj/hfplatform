package com.marekmaj.hfplatform.service.impl;

import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.service.AccountService;
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
        final double balance = balanceAccountCommand.getAccount().getBalance();
        resultEventPublisher.getNextResultEventAndPublishSuccess(balanceAccountCommand.getId(), balance);
        return balance;
    }

    @Override
    public boolean transfer(final TransferAccountCommand transferAccountCommand) {
        if (transferAccountCommand.getFrom() == transferAccountCommand.getTo()){
            Stats.increaseCanceled();
            resultEventPublisher.getNextResultEventAndPublishFailed(transferAccountCommand.getId());
            return false;
        }

        try {
            STM.atomic(new Runnable() {
                @Override
                public void run() {
                    transferAccountCommand.getFrom().decreaseBalance(transferAccountCommand.getAmount());
                    transferAccountCommand.getTo().increaseBalance(transferAccountCommand.getAmount());

                    final ResultEvent resultEvent = resultEventPublisher.getNextResultEvent();
                    resultEvent.getResult().setId(transferAccountCommand.getId());

                    STM.afterRollback(new RollbackAction(resultEventPublisher, resultEvent));

                    STM.afterCommit(new CommitAction(resultEventPublisher, resultEvent, transferAccountCommand.getAmount()));
                }
            });

            return true;
        } catch (InsufficientFundsException e){
            Stats.increaseInsufficient();
            resultEventPublisher.getNextResultEventAndPublishFailed(transferAccountCommand.getId());
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
        resultEventPublisher.publishEvent();
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
        resultEvent.getResult().setStatus(true);
        resultEvent.getResult().setAmount(amount);
        resultEventPublisher.publishEvent();
        Stats.increaseCommits();
    }
}