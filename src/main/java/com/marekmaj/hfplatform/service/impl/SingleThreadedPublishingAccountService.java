package com.marekmaj.hfplatform.service.impl;

import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.Result;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.InsufficientFundsException;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class SingleThreadedPublishingAccountService implements AccountService {

    final ResultEventPublisher resultEventPublisher;

    public SingleThreadedPublishingAccountService(final ResultEventPublisher resultEventPublisher) {
        this.resultEventPublisher = resultEventPublisher;
    }

    @Override
    public double checkBalance(BalanceAccountCommand balanceAccountCommand) {
        double balance = balanceAccountCommand.getAccount().getBalance();
        resultEventPublisher.getNextResultEventAndPublish(new Result(true, balance), balanceAccountCommand.getId());
        return balance;
    }

    @Override
    public boolean transfer(TransferAccountCommand transferAccountCommand) {
        if (transferAccountCommand.getFrom() == transferAccountCommand.getTo()){
            Stats.increaseCanceled();
            resultEventPublisher.getNextResultEventAndPublish(new Result(false, Double.NaN), transferAccountCommand.getId());
            return false;
        }

        try {
            transferAccountCommand.getFrom().decreaseBalance(transferAccountCommand.getAmount());
            transferAccountCommand.getTo().increaseBalance(transferAccountCommand.getAmount());
            resultEventPublisher.getNextResultEventAndPublish(new Result(true, transferAccountCommand.getAmount()), transferAccountCommand.getId());
            return true;
        } catch (InsufficientFundsException e){
            Stats.increaseInsufficient();
            resultEventPublisher.getNextResultEventAndPublish(new Result(false, Double.NaN), transferAccountCommand.getId());
        }
        return false;
    }
}