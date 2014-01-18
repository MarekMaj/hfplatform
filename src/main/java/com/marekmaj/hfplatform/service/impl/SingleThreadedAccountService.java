package com.marekmaj.hfplatform.service.impl;

import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.service.model.InsufficientFundsException;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class SingleThreadedAccountService implements AccountService{

    @Override
    public double checkBalance(BalanceAccountCommand balanceAccountCommand) {
        return balanceAccountCommand.getAccount().getBalance();
    }

    @Override
    public boolean transfer(TransferAccountCommand transferAccountCommand) {
        if (transferAccountCommand.getFrom() == transferAccountCommand.getTo()){
            Stats.increaseCanceled();
            return false;
        }

        try {
            transferAccountCommand.getFrom().decreaseBalance(transferAccountCommand.getAmount());
            transferAccountCommand.getTo().increaseBalance(transferAccountCommand.getAmount());
            return true;
        } catch (InsufficientFundsException e){
            Stats.increaseInsufficient();
        }
        return false;
    }
}
