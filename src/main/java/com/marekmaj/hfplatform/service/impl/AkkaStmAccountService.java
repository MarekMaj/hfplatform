package com.marekmaj.hfplatform.service.impl;


import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.service.model.InsufficientFundsException;
import com.marekmaj.hfplatform.utils.Stats;
import net.jcip.annotations.ThreadSafe;
import scala.concurrent.stm.japi.STM;

@ThreadSafe
public class AkkaStmAccountService implements AccountService {

    @Override
    public double checkBalance(Account account) {
        return account.getBalance();
    }

    @Override
    public boolean transfer(final Account from, final Account to, final double amount) {
        if (from == to){
            Stats.increaseCanceled();
            return false;
        }

        try {
            STM.atomic(new Runnable() {
                @Override
                public void run() {
                    from.decreaseBalance(amount);
                    to.increaseBalance(amount);

                    STM.afterRollback(new Runnable() {
                        @Override
                        public void run() {
                            Stats.increaseRollbacks();
                        }
                    });

                    STM.afterCommit(new Runnable() {
                        public void run() {
                            Stats.increaseCommits();
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
