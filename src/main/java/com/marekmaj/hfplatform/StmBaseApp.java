package com.marekmaj.hfplatform;

import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.service.model.StmAccount;
import net.openhft.affinity.AffinityThreadFactory;

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
        accountEventPublisher = new AccountEventPublisher(cyclicBarrier, inputDisruptor, ITERATIONS, accounts);
    }

    protected static final int NUM_WORKERS = Integer.getInteger("stm.threads", 5);
    {
        System.out.println(getClass().getSimpleName() +": NUM_WORKERS: " + NUM_WORKERS);
    }
    protected final ExecutorService WORKERS_EXECUTOR = AFFINITY ?
            Executors.newFixedThreadPool(NUM_WORKERS, new AffinityThreadFactory("WORKERS_EXECUTOR")) :
            Executors.newFixedThreadPool(NUM_WORKERS);

}
