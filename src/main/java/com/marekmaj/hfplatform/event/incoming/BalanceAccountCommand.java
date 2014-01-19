package com.marekmaj.hfplatform.event.incoming;


import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.WithID;

public final class BalanceAccountCommand extends WithID implements AccountCommand {

    private Account account;

    public BalanceAccountCommand(int id) {
        super(id);
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public void execute(final AccountService accountService) {
        accountService.checkBalance(this);
    }
}
