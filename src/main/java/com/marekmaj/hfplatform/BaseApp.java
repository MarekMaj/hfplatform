package com.marekmaj.hfplatform;

import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.event.incoming.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.Stats;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;


public abstract class BaseApp {
    protected static final int INPUT_DISRUPTOR_SIZE = 1024 * 64;
    protected static final int OUTPUT_DISRUPTOR_SIZE = 1024 * 1024 * 16;
    protected static final int NUM_ACCOUNTS = 100;
    protected static final long ITERATIONS = 1000L* 1000L * 10L;
    protected static final long WARMUP = 1000L * 1000L * 10L;
    protected static final double INITIAL_BALANCE = 100000;

    protected static final int GATEWAY_PUBLISHERS_COUNT = 1;
    protected static final int GATEWAY_CONSUMERS_COUNT = 1;

    protected Account[] accounts;

    protected final CyclicBarrier cyclicBarrier = new CyclicBarrier(GATEWAY_PUBLISHERS_COUNT + 1);

    protected final RingBuffer<AccountEvent> inputDisruptor =
            RingBuffer.createSingleProducer(AccountEvent.ACCOUNT_EVENT_FACTORY,
                    INPUT_DISRUPTOR_SIZE,
                    new BusySpinWaitStrategy());  // TODO bound threads to cores

    protected final AccountEventPublisher[] accountEventPublishers = new AccountEventPublisher[GATEWAY_PUBLISHERS_COUNT];


    protected TransferAccountCommand createRandomTransferAccountCommand(){
        return new TransferAccountCommand(accounts[ThreadLocalRandom.current().nextInt(NUM_ACCOUNTS)],
                accounts[ThreadLocalRandom.current().nextInt(NUM_ACCOUNTS)],
                ThreadLocalRandom.current().nextDouble(20));
    }

    protected BalanceAccountCommand createRandomBalanceAccountCommand(){
        return new BalanceAccountCommand(accounts[ThreadLocalRandom.current().nextInt(NUM_ACCOUNTS)]);
    }

    protected IndexedChronicle chronicle;
    {
        try {
            this.chronicle = new IndexedChronicle("/dane/work/mgr/logs/log");
            chronicle.useUnsafe(true); // for benchmarks
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void run() throws Exception{
        System.out.println( "Starting transactions..." );
        //warmup();
        //System.gc();
        long start = System.currentTimeMillis();
        start();
        long opsPerSecond = (ITERATIONS * GATEWAY_PUBLISHERS_COUNT * 1000L) / (System.currentTimeMillis() - start);
        chronicle.close();
        showStats(opsPerSecond);
        showStatsSpecific();

        System.exit(0);
    }

    protected void showStats(long opsPerSecond){
        System.out.println( "Initial whole balance: " + INITIAL_BALANCE*NUM_ACCOUNTS);
        System.out.println( "Current balance: " + sumBalance());
        System.out.println( "Ops per second: " + opsPerSecond);
        System.out.println( "Rollbacks for transfer operation: " + Stats.getTransactionRollbacks());
        System.out.println( "Rollbacks for account manipulation: " + Stats.getAccountRollbacks());
        System.out.println( "Comitted: " + Stats.getCommits());
        System.out.println( "Canceled (the same account): " + Stats.getCanceled());
        System.out.println( "Canceled (insufficient funds): " + Stats.getInsufficient());
    }

    private double sumBalance(){
        double balance = 0.0;
        for(Account account: accounts){
            balance += account.getBalance();
        }
        return balance;
    }

    protected abstract void start() throws Exception;

    protected abstract void warmup() throws Exception;

    protected abstract void showStatsSpecific();
}
