package com.marekmaj.hfplatform;

import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.impl.AkkaStmAccountService;
import com.marekmaj.hfplatform.service.model.StmAccount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class StmApp extends BaseApp{

    {
        accounts = new StmAccount[NUM_ACCOUNTS];
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            accounts[i] = new StmAccount(INITIAL_BALANCE);
        }
    }

    private static final int NUM_WORKERS = 5;
    private final ExecutorService WORKERS_EXECUTOR = Executors.newFixedThreadPool(NUM_WORKERS);
    private final ExecutorService PUBLISHERS_EXECUTOR = Executors.newSingleThreadExecutor();
    private final AccountEventWorkHandler[] handlers = new AccountEventWorkHandler[NUM_WORKERS];
    {
        for (int i = 0; i < NUM_WORKERS; i++){
            handlers[i] = new AccountEventWorkHandler(new AkkaStmAccountService());  // TODO this will need something more
        }
    }
    {
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            accountEventPublishers[i] = new AccountEventPublisher(cyclicBarrier, inputDisruptor, ITERATIONS, accounts);
        }
    }

    private final WorkerPool<AccountEvent> workerPool =
            new WorkerPool<AccountEvent>(inputDisruptor,
                    inputDisruptor.newBarrier(),
                    new FatalExceptionHandler(),
                    handlers);

    {
        inputDisruptor.addGatingSequences(workerPool.getWorkerSequences());
    }

    public static void main( String[] args ) throws Exception{
        new StmApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork(ITERATIONS);
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

    private void startWork(final long iterations) throws Exception{
        RingBuffer<AccountEvent> ringBuffer = workerPool.start(WORKERS_EXECUTOR);

        Future<?>[] futures = new Future[GATEWAY_PUBLISHERS_COUNT];
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i] = PUBLISHERS_EXECUTOR.submit(accountEventPublishers[i]);
        }

        cyclicBarrier.await();

        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
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
