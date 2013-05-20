package com.marekmaj.hfplatform.service.impl;

import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.service.model.InsufficientFundsException;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class SingleThreadedAccountService implements AccountService{

    @Override
    public double checkBalance(Account account) {
        return account.getBalance();
    }

    @Override
    public boolean transfer(Account from, Account to, double amount) {
        try {
            from.decreaseBalance(amount);
            to.increaseBalance(amount);
            return true;
        } catch (InsufficientFundsException e){
        }
        return false;
    }
}
