package com.marekmaj.hfplatform;

import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.impl.AkkaStmAccountService;

import java.util.concurrent.Future;


public class StmApp extends StmBaseApp {

    private final AccountEventWorkHandler[] accountEventWorkHandlers = new AccountEventWorkHandler[NUM_WORKERS];
    {
        for (int i = 0; i < NUM_WORKERS; i++){
            accountEventWorkHandlers[i] = new AccountEventWorkHandler(new AkkaStmAccountService());  // TODO this will need something more
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
/*

    private void startWorkOld(final long iterations) throws Exception{
        RingBuffer<AccountEvent> inputDisruptor = workerPool.start(WORKERS_EXECUTOR);

        for (long i = 0; i < iterations; i++) {
            long sequence = inputDisruptor.next();
            inputDisruptor.get(sequence).setAccountCommand(createRandomTransferAccountCommand());
            inputDisruptor.publish(sequence);
        }

        workerPool.drainAndHalt();
    }
*/

    private void startWork() throws Exception{
        workerPool.start(WORKERS_EXECUTOR);

        Future<?>[] futures = new Future[GATEWAY_PUBLISHERS_COUNT];
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i] = GATEWAY_PUBLISHERS_EXECUTOR.submit(accountEventPublishers[i]);
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
