package com.marekmaj.hfplatform.service.impl;


import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.service.model.InsufficientFundsException;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.ThreadSafe;
import scala.concurrent.stm.japi.STM;

@ThreadSafe
public class AkkaStmAccountService implements AccountService {

    @Override
    public double checkBalance(BalanceAccountCommand balanceAccountCommand) {
        return balanceAccountCommand.getAccount().getBalance();
    }

    @Override
    public boolean transfer(final TransferAccountCommand transferAccountCommand) {
        if (transferAccountCommand.getFrom() == transferAccountCommand.getTo()){
            Stats.increaseCanceled();
            return false;
        }

        try {
            STM.atomic(new Runnable() {
                @Override
                public void run() {
                    transferAccountCommand.getFrom().decreaseBalance(transferAccountCommand.getAmount());
                    transferAccountCommand.getTo().increaseBalance(transferAccountCommand.getAmount());

                    STM.afterRollback(new Runnable() {
                        @Override
                        public void run() {
                            Stats.increaseRollbacks();
                        }
                    });

                    STM.afterCommit(new Runnable() {
                        public void run() {
                        }
                    });
                }
            });
            return true;
        } catch (InsufficientFundsException e){
            Stats.increaseInsufficient();
        }

        return false;
    }
}
