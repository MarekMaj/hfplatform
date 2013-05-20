package com.marekmaj.hfplatform;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.marekmaj.hfplatform.event.AccountEvent;
import com.marekmaj.hfplatform.event.AccountEventPublisher;
import com.marekmaj.hfplatform.event.BalanceAccountCommand;
import com.marekmaj.hfplatform.event.TransferAccountCommand;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.Stats;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;


public abstract class BaseApp {
    protected static final int BUFFER_SIZE = 1024 * 64;
    protected static final int NUM_ACCOUNTS = 100;
    protected static final long ITERATIONS = 1000L * 1000L * 100L;
    protected static final long WARMUP = 1000L * 1000L * 10L;
    protected static final double INITIAL_BALANCE = 100000;

    protected static final int NUM_PUBLISHERS = 1;

    protected Account[] accounts;

    protected final CyclicBarrier cyclicBarrier = new CyclicBarrier(NUM_PUBLISHERS + 1);

    protected final RingBuffer<AccountEvent> ringBuffer =
            RingBuffer.createSingleProducer(AccountEvent.ACCOUNT_EVENT_FACTORY,
                    BUFFER_SIZE,
                    new YieldingWaitStrategy());  // TODO bound threads to cores

    protected final AccountEventPublisher[] accountEventPublishers = new AccountEventPublisher[NUM_PUBLISHERS];


    protected TransferAccountCommand createRandomTransferAccountCommand(){
        return new TransferAccountCommand(accounts[ThreadLocalRandom.current().nextInt(NUM_ACCOUNTS)],
                accounts[ThreadLocalRandom.current().nextInt(NUM_ACCOUNTS)],
                ThreadLocalRandom.current().nextDouble(20));
    }

    protected BalanceAccountCommand createRandomBalanceAccountCommand(){
        return new BalanceAccountCommand(accounts[ThreadLocalRandom.current().nextInt(NUM_ACCOUNTS)]);
    }

    protected void run() throws Exception{
        System.out.println( "Starting transactions..." );
        //warmup();
        //System.gc();
        long start = System.currentTimeMillis();
        start();
        long opsPerSecond = (ITERATIONS * NUM_PUBLISHERS * 1000L) / (System.currentTimeMillis() - start);
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
