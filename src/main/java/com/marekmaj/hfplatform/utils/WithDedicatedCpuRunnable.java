package com.marekmaj.hfplatform.utils;


import net.openhft.affinity.AffinityLock;

public class WithDedicatedCpuRunnable implements Runnable {

    private final Runnable runnable;
    private final AffinityLock affinityLock;

    public WithDedicatedCpuRunnable(Runnable runnable) {
        this.runnable = runnable;
        this.affinityLock = AffinityLock.acquireLock(false);
    }

    @Override
    public void run() {
        affinityLock.bind();
        try {
            runnable.run();
        } catch (Exception e) {
        } finally {
            affinityLock.release();
        }
    }
}
