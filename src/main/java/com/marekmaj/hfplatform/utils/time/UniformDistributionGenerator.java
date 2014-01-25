package com.marekmaj.hfplatform.utils.time;


import com.marekmaj.hfplatform.utils.Stats;

public class UniformDistributionGenerator implements TimeDelayGenerator {

    private static final int MEAN_TIME = Integer.getInteger("time.mean", 1000);
    {
        System.out.println(getClass().getSimpleName() +": MEAN_TIME: " + MEAN_TIME);
    }

    @Override
    public void initTimes(int iterations) {
        for (int i=0; i<iterations; i++) {
            Stats.delaysBeforeLatenciesAfter[i] = MEAN_TIME;
        }
    }
}
