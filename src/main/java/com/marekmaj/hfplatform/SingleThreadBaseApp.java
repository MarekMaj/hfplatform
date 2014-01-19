package com.marekmaj.hfplatform;

import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.service.model.SingleThreadedAccount;

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
}
