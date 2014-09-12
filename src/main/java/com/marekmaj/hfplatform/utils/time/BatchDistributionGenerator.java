package com.marekmaj.hfplatform.utils.time;


import com.marekmaj.hfplatform.utils.Stats;

public class BatchDistributionGenerator implements TimeDelayGenerator {

    private static final int MEAN_TIME = Integer.getInteger("time.mean", 1_000_000);
    private static final int BATCH_SIZE = Integer.getInteger("time.batch", 1_000);
    {
        System.out.println(getClass().getSimpleName() +": MEAN_TIME: " + MEAN_TIME);
        System.out.println(getClass().getSimpleName() +": BATCH_SIZE: " + BATCH_SIZE);
    }

    @Override
    public void initTimes(int iterations) {
        int i = 0;
        while (i < iterations) {
            Stats.delaysBeforeLatenciesAfter[i++] = MEAN_TIME;
            for (int j = 1; j < BATCH_SIZE && i < iterations; j++, i++) {
                Stats.delaysBeforeLatenciesAfter[i] = 0;
            }
        }
    }
}

