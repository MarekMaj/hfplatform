package com.marekmaj.hfplatform;


import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.AccountService;
import com.marekmaj.hfplatform.service.impl.SingleThreadedAccountService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SingleThreadApp extends SingleThreadBaseApp {

    private static final int NUM_WORKERS = 1;
    private final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final ExecutorService EXECUTOR2 = Executors.newSingleThreadExecutor();
    private final AccountService accountService = new SingleThreadedAccountService();
    private final AccountEventWorkHandler[] accountEventWorkHandlers = new AccountEventWorkHandler[NUM_WORKERS];
    {
        for (int i = 0; i < NUM_WORKERS; i++){
            accountEventWorkHandlers[i] = new AccountEventWorkHandler(accountService);  // TODO this will need something more
        }
    }
    private final WorkerPool<AccountEvent> workerPool = getAccountEventWorkerPool(accountEventWorkHandlers);

    public static void main( String[] args ) throws Exception{
        new SingleThreadApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork();
    }

    private void startWork() throws Exception{
        RingBuffer<AccountEvent> ringBuffer = workerPool.start(EXECUTOR);

        Future<?>[] futures = new Future[GATEWAY_PUBLISHERS_COUNT];
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i] = EXECUTOR2.submit(accountEventPublishers[i]);
        }

        cyclicBarrier.await();

        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i].get();
        }

        workerPool.drainAndHalt();
    }

    @Override
    protected void warmup() throws Exception {
        startWork();
    }

    @Override
    protected void showStatsSpecific() {
        for (AccountEventWorkHandler handler : accountEventWorkHandlers){
            System.out.println( "Total ops for handler " + handler.getCounter());
        }
    }
}
