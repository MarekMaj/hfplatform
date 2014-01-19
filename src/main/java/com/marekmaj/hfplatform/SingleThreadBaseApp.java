package com.marekmaj.hfplatform;

import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.service.model.SingleThreadedAccount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SingleThreadBaseApp extends BaseApp {

    {
        accounts = new SingleThreadedAccount[NUM_ACCOUNTS];
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            accounts[i] = new SingleThreadedAccount(INITIAL_BALANCE);
        }
    }
    {
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            accountEventPublishers[i] = new AccountEventPublisher(cyclicBarrier, inputDisruptor, ITERATIONS, accounts);
        }
    }

    protected static final int NUM_WORKERS = 1;
    protected final ExecutorService WORKER_EXECUTOR = Executors.newSingleThreadExecutor();
}
