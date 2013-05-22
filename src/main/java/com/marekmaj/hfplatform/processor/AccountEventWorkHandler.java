package com.marekmaj.hfplatform.processor;

import com.lmax.disruptor.WorkHandler;
import com.marekmaj.hfplatform.event.AccountEvent;
import com.marekmaj.hfplatform.event.Result;
import com.marekmaj.hfplatform.service.AccountService;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
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
        Result result = accountEvent.executeWith(accountService);
        counter++;
        // TODO what to do with result
    }
}
