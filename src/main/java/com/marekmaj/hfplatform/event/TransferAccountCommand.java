package com.marekmaj.hfplatform.event;


import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;

public final class TransferAccountCommand implements AccountCommand {

    private Account from;
    private Account to;
    private double amount;

    public TransferAccountCommand(final Account from, final Account to, final double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public Result execute(final AccountService accountService) {
        boolean status = accountService.transfer(from, to, amount);
        return new Result(status, 0.00);
    }
}
