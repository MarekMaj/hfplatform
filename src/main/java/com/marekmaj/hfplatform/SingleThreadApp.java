package com.marekmaj.hfplatform;


import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.AccountEvent;
import com.marekmaj.hfplatform.event.AccountEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.impl.SingleThreadedAccountService;
import com.marekmaj.hfplatform.service.model.SingleThreadedAccount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SingleThreadApp extends BaseApp {
    {
        accounts = new SingleThreadedAccount[NUM_ACCOUNTS];
        for (int i = 0; i < NUM_ACCOUNTS; i++)
        {
            accounts[i] = new SingleThreadedAccount(INITIAL_BALANCE);
        }
    }

    private static final int NUM_WORKERS = 1;
    private final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final ExecutorService EXECUTOR2 = Executors.newSingleThreadExecutor();
    private final AccountService accountService = new SingleThreadedAccountService();
    private final AccountEventWorkHandler[] handlers = new AccountEventWorkHandler[NUM_WORKERS];
    {
        for (int i = 0; i < NUM_WORKERS; i++){
            handlers[i] = new AccountEventWorkHandler(accountService);  // TODO this will need something more
        }
    }
    {
        for (int i = 0; i < NUM_PUBLISHERS; i++) {
            accountEventPublishers[i] = new AccountEventPublisher(cyclicBarrier, ringBuffer, ITERATIONS, accounts);
        }
    }

    private final WorkerPool<AccountEvent> workerPool =
            new WorkerPool<AccountEvent>(ringBuffer,
                    ringBuffer.newBarrier(),
                    new FatalExceptionHandler(),
                    handlers);


    public static void main( String[] args ) throws Exception{
        new SingleThreadApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork(ITERATIONS);
    }

    private void startWork(final long iterations) throws Exception{
        RingBuffer<AccountEvent> ringBuffer = workerPool.start(EXECUTOR);

        Future<?>[] futures = new Future[NUM_PUBLISHERS];
        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            futures[i] = EXECUTOR2.submit(accountEventPublishers[i]);
        }

        cyclicBarrier.await();

        for (int i = 0; i < NUM_PUBLISHERS; i++) {
            futures[i].get();
        }

        workerPool.drainAndHalt();
    }

    @Override
    protected void warmup() throws Exception {
        startWork(WARMUP);
    }

    @Override
    protected void showStatsSpecific() {
        for (AccountEventWorkHandler handler : handlers){
            System.out.println( "Total ops for handler " + handler.getCounter());
        }
    }
}
