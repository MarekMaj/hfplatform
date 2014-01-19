package com.marekmaj.hfplatform.event.incoming;


import com.lmax.disruptor.EventFactory;

public final class AccountEvent {

    private AccountCommand accountCommand;

    public AccountEvent(AccountCommand accountCommand) {
        this.accountCommand = accountCommand;
    }

    public AccountCommand getAccountCommand() {
        return accountCommand;
    }

    public final static EventFactory<AccountEvent> TRANSFER_EVENT_FACTORY = new EventFactory<AccountEvent>() {
        public AccountEvent newInstance() {
            return new AccountEvent(new TransferAccountCommand(-1));
        }
    };

    public final static EventFactory<AccountEvent> BALANCE_EVENT_FACTORY = new EventFactory<AccountEvent>() {
        public AccountEvent newInstance() {
            return new AccountEvent(new BalanceAccountCommand(-1));
        }
    };
}
