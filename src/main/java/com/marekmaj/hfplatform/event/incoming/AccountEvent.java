package com.marekmaj.hfplatform.event.incoming;


import com.lmax.disruptor.EventFactory;

public final class AccountEvent {

    private AccountCommand accountCommand;

    public AccountCommand getAccountCommand() {
        return accountCommand;
    }

    public void setAccountCommand(AccountCommand accountCommand) {
        this.accountCommand = accountCommand;
    }

    public final static EventFactory<AccountEvent> ACCOUNT_EVENT_FACTORY = new EventFactory<AccountEvent>() {
        public AccountEvent newInstance() {
            return new AccountEvent();
        }
    };
}
