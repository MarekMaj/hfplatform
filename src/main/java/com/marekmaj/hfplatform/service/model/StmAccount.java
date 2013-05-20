package com.marekmaj.hfplatform.service.model;

import com.marekmaj.hfplatform.utils.Stats;
import scala.concurrent.stm.Ref;
import scala.concurrent.stm.japi.STM;

// TODO make account independent from processing strategy
public class StmAccount extends Account {

    final private Ref.View<Double> balance;

    public StmAccount(double balance) {
        this.balance = STM.newRef(balance);
    }

    @Override
    public double getBalance() {
        return balance.get();
    }

    @Override
    public void decreaseBalance(final double change) throws InsufficientFundsException {
        STM.atomic(new Runnable() {
            @Override
            public void run() {
                final double current = balance.get();
                if (current < change){
                    throw new InsufficientFundsException();
                }
                balance.swap(current - change);

                STM.afterRollback(new Runnable() {
                    @Override
                    public void run() {
                        Stats.increaseAccountRollbacks();
                    }
                });
            }
        });
    }

    @Override
    public void increaseBalance(final double change) {
        STM.atomic(new Runnable() {
            @Override
            public void run() {
                balance.swap(balance.get() + change);

                STM.afterRollback(new Runnable() {
                    @Override
                    public void run() {
                        Stats.increaseAccountRollbacks();
                    }
                });
            }
        });
    }
}

