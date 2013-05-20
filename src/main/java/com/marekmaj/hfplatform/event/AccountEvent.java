package com.marekmaj.hfplatform.event;


import com.lmax.disruptor.EventFactory;
import com.marekmaj.hfplatform.service.AccountService;

public final class AccountEvent {

    private AccountCommand accountCommand;

    public AccountCommand getAccountCommand() {
        return accountCommand;
    }

    public void setAccountCommand(AccountCommand accountCommand) {
        this.accountCommand = accountCommand;
    }

    public Result executeWith(final AccountService accountService){
        return accountCommand.execute(accountService);
    }

    public final static EventFactory<AccountEvent> ACCOUNT_EVENT_FACTORY = new EventFactory<AccountEvent>() {
        public AccountEvent newInstance() {
            return new AccountEvent();
        }
    };
}
