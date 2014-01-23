package com.marekmaj.hfplatform;


import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.impl.SingleThreadedAccountService;

import java.util.concurrent.Future;

public class SingleThreadApp extends SingleThreadBaseApp {

    private final AccountEventWorkHandler accountEventWorkHandler = new AccountEventWorkHandler(new SingleThreadedAccountService());
    private final WorkerPool<AccountEvent> workerPool = getAccountEventWorkerPool(accountEventWorkHandler);

    public static void main( String[] args ) throws Exception{
        new SingleThreadApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork();
    }

    private void startWork() throws Exception{
        workerPool.start(WORKER_EXECUTOR);

        Future<?> future = GATEWAY_PUBLISHER_EXECUTOR.submit(accountEventPublisher);

        cyclicBarrier.await();

        future.get();

        workerPool.drainAndHalt();
    }

    @Override
    protected void warmup() throws Exception {
        startWork();
    }

    @Override
    protected void showStatsSpecific() {
        System.out.println( "Total ops for handler " + accountEventWorkHandler.getCounter());
    }
}
