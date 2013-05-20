package com.marekmaj.hfplatform.service;

import com.marekmaj.hfplatform.service.model.Account;

public interface AccountService {

    public double checkBalance(Account account);

    public boolean transfer(Account from, Account to, double amount);
}