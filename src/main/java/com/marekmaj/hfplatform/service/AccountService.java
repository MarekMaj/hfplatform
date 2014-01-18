package com.marekmaj.hfplatform.service;

import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;

public interface AccountService {

    public double checkBalance(BalanceAccountCommand balanceAccountCommand);

    public boolean transfer(TransferAccountCommand transferAccountCommand);
}