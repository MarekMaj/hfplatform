package com.marekmaj.hfplatform.event.incoming;


import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.WithID;

public final class TransferAccountCommand extends WithID implements AccountCommand {

    private Account from;
    private Account to;
    private double amount;

    public TransferAccountCommand(final int id) {
        super(id);
    }

    public Account getFrom() {
        return from;
    }

    public Account getTo() {
        return to;
    }

    public double getAmount() {
        return amount;
    }

    public void setFrom(Account from) {
        this.from = from;
    }

    public void setTo(Account to) {
        this.to = to;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(final AccountService accountService) {
        accountService.transfer(this);
    }
}
