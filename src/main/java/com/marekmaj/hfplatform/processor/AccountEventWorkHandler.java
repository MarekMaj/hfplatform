package com.marekmaj.hfplatform.processor;

import com.lmax.disruptor.WorkHandler;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.incoming.Result;
import com.marekmaj.hfplatform.service.AccountService;


public class AccountEventWorkHandler implements WorkHandler<AccountEvent> {

    private final AccountService accountService;
    private Long counter = 0L;

    public AccountEventWorkHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    public Long getCounter() {
        return counter;
    }

    @Override
    public void onEvent(final AccountEvent accountEvent) throws Exception {
        // instead handling result here, publish it directly in service
        Result result = accountEvent.executeWith(accountService);
        counter++;
    }
}
