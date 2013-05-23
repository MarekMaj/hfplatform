package com.marekmaj.hfplatform.event.incoming;


import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;

public final class BalanceAccountCommand implements AccountCommand {

    private final Account account;

    public BalanceAccountCommand(final Account account) {
        this.account = account;
    }

    @Override
    public Result execute(final AccountService accountService) {
        double balance = accountService.checkBalance(account);
        return new Result(true, balance);
    }
}
