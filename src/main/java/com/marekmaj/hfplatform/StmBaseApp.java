package com.marekmaj.hfplatform;

import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.service.model.StmAccount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class StmBaseApp extends BaseApp {

    {
        accounts = new StmAccount[NUM_ACCOUNTS];
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            accounts[i] = new StmAccount(INITIAL_BALANCE);
        }
    }
    {
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            accountEventPublishers[i] = new AccountEventPublisher(cyclicBarrier, inputDisruptor, ITERATIONS, accounts);
        }
    }

    protected static final int NUM_WORKERS = 6;
    protected final ExecutorService WORKERS_EXECUTOR = Executors.newFixedThreadPool(NUM_WORKERS);

}
