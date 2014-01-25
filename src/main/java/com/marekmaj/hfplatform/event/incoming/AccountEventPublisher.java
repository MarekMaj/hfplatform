package com.marekmaj.hfplatform.event.incoming;

import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.Stats;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;


public final class AccountEventPublisher implements Runnable {
    private final CyclicBarrier cyclicBarrier;
    private final RingBuffer<AccountEvent> ringBuffer;
    private final int iterations;
    private final Account[] accounts;

    private static final boolean WAIT_FOR_GENERATED_TIME = System.getProperties().containsKey("time.gen")
            && !System.getProperty("time.gen").equals("${time.gen}") && !System.getProperty("time.gen").equals("none");
    static {
        System.out.println(AccountEventPublisher.class.getSimpleName() +": WAIT_FOR_GENERATED_TIME: " + WAIT_FOR_GENERATED_TIME);
    }

    private long cumulativeExpectedTimeTillLastEvent;

    public AccountEventPublisher(final CyclicBarrier cyclicBarrier,
                                 final RingBuffer<AccountEvent> ringBuffer,
                                 final int iterations,
                                 final Account[] accounts) {
        this.cyclicBarrier = cyclicBarrier;
        this.ringBuffer = ringBuffer;
        this.iterations = iterations;
        this.accounts = accounts;
    }

    @Override
    public void run() {
        try {
            cyclicBarrier.await();
            cumulativeExpectedTimeTillLastEvent = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                long sequence = ringBuffer.next();
                AccountEvent event = ringBuffer.get(sequence);
                generateAccountCommand(i, event.getAccountCommand());
                if (WAIT_FOR_GENERATED_TIME) {
                    busyWaitForEventStartTime(i);
                } else {
                    Stats.delaysBeforeLatenciesAfter[i] = System.nanoTime();
                }
                ringBuffer.publish(sequence);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void busyWaitForEventStartTime(int i) {
        this.cumulativeExpectedTimeTillLastEvent += Stats.delaysBeforeLatenciesAfter[i];
        Stats.delaysBeforeLatenciesAfter[i] = cumulativeExpectedTimeTillLastEvent;
        while(System.nanoTime() < cumulativeExpectedTimeTillLastEvent) {
            // busy wait
        }
    }

    private void generateAccountCommand(int i, AccountCommand accountCommand) {
        if (accountCommand instanceof BalanceAccountCommand) {
            ((BalanceAccountCommand) accountCommand).setAccount(getRandomAccount());
            ((BalanceAccountCommand) accountCommand).setId(i);
        } else {
            ((TransferAccountCommand) accountCommand).setFrom(getRandomAccount());
            ((TransferAccountCommand) accountCommand).setTo(getRandomAccount());
            ((TransferAccountCommand) accountCommand).setAmount(ThreadLocalRandom.current().nextDouble(20));
            ((TransferAccountCommand) accountCommand).setId(i);
        }
    }

    private Account getRandomAccount() {
        return accounts[ThreadLocalRandom.current().nextInt(accounts.length)];
    }

}