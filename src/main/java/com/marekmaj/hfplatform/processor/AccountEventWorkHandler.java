package com.marekmaj.hfplatform.processor;

import com.lmax.disruptor.WorkHandler;
import com.marekmaj.hfplatform.event.AccountEvent;
import com.marekmaj.hfplatform.event.Result;
import com.marekmaj.hfplatform.service.AccountService;

import java.util.concurrent.atomic.AtomicLong;


public class AccountEventWorkHandler implements WorkHandler<AccountEvent> {

    private final AccountService accountService;
    private final AtomicLong counter = new AtomicLong();

    public AccountEventWorkHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    public AtomicLong getCounter() {
        return counter;
    }

    @Override
    public void onEvent(final AccountEvent accountEvent) throws Exception {
        Result result = accountEvent.executeWith(accountService);
        counter.incrementAndGet();
        // TODO what to do with result
    }
}
