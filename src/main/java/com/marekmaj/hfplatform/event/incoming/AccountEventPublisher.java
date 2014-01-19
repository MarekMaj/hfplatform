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

            // TODO spróbować generowac event co jakiś czas np. 100ns
            // TODO przemyslec jak zmienil by sie model gdyby mierzyc start time przed ringBuffer.gets
            for (int i = 0; i < iterations; i++) {
                long sequence = ringBuffer.next();
                AccountEvent event = ringBuffer.get(sequence);
                generateAccountCommand(i, event.getAccountCommand());
                Stats.startTimes[i] = System.nanoTime();
                ringBuffer.publish(sequence);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
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