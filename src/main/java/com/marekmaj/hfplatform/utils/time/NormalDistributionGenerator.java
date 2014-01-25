package com.marekmaj.hfplatform.utils.time;


import com.marekmaj.hfplatform.utils.Stats;

import java.util.Random;

public class NormalDistributionGenerator implements TimeDelayGenerator {

    private Random random = new Random();
    private static final int STD_DEV = Integer.getInteger("time.std", 100);
    private static final int MEAN_TIME = Integer.getInteger("time.mean", 1000);
    {
        System.out.println(getClass().getSimpleName() +": STD_DEV: " + STD_DEV);
        System.out.println(getClass().getSimpleName() +": MEAN_TIME: " + MEAN_TIME);
    }

    @Override
    public void initTimes(int iterations) {
        for (int i=0; i<iterations; i++) {
            Stats.delaysBeforeLatenciesAfter[i] = ((long) random.nextGaussian()*STD_DEV) + MEAN_TIME;
        }
    }
}
