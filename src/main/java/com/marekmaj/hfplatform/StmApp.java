package com.marekmaj.hfplatform;

import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.impl.AkkaStmAccountService;
import com.marekmaj.hfplatform.utils.MinorStatsPrinter;

import java.util.concurrent.Future;


public class StmApp extends StmBaseApp {

    private final AccountEventWorkHandler[] accountEventWorkHandlers = new AccountEventWorkHandler[NUM_WORKERS];
    {
        for (int i = 0; i < NUM_WORKERS; i++){
            accountEventWorkHandlers[i] = new AccountEventWorkHandler(new AkkaStmAccountService());
        }
    }
    private final WorkerPool<AccountEvent> workerPool = getAccountEventWorkerPool(accountEventWorkHandlers);

    public static void main( String[] args ) throws Exception{
        new StmApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork();
    }

    private void startWork() throws Exception {
        Future<?> future = GATEWAY_PUBLISHER_EXECUTOR.submit(accountEventPublisher);
        workerPool.start(WORKERS_EXECUTOR);

        cyclicBarrier.await();

        future.get();

        workerPool.drainAndHalt();
    }

    @Override
    protected void showStatsSpecific() {
        MinorStatsPrinter.printAccountEventHandlersStats(accountEventWorkHandlers);
    }
}
