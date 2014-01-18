package com.marekmaj.hfplatform.event.incoming;


import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.WithID;

public final class TransferAccountCommand extends WithID implements AccountCommand {

    private Account from;
    private Account to;
    private double amount;

    public TransferAccountCommand(final int eventId, final Account from, final Account to, final double amount) {
        super(eventId);
        this.from = from;
        this.to = to;
        this.amount = amount;
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

    @Override
    public void execute(final AccountService accountService) {
        accountService.transfer(this);
    }
}
