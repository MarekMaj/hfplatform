package com.marekmaj.hfplatform.utils;


import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramData;

public final class HistogramPrinter {

    public static void showHistogram(int start, int end) {
        System.out.println();
        System.out.println( "-------------STATS COLLECTED-------------");
        long min = getDiffInStats(start);
        long max = getDiffInStats(start);

        for (int i = start; i < end; i++) {
            long diff = getDiffInStats(i);
            if (diff < min) {
                min = diff;
            } else if (diff > max) {
                max = diff;
            }
        }
        System.out.println( "-------------MIN_MAX[nano s]-------------");
        System.out.println("Minimum latency[ns]: " + min);
        System.out.println("Maximum latency[ns]: " + max);

        for (int i = start; i < end; i++) {
            if (getDiffInStats(i) > 100_000_000) {
                Stats.delaysBeforeLatenciesAfter[i] = 100_000_000;
            }
        }
        System.out.println();
        System.out.println( "-------------HISTOGRAM[micro s]-------------");
        Histogram histogram = new Histogram(100_000, 5);
        for (int i = start; i < end; i++) {
            histogram.recordValue(getDiffInStats(i)/1000);
        }

        HistogramData data = histogram.getHistogramData();
        //System.out.println( "Max time " + data.getMaxValue());
        System.out.println( "Min time " + data.getMinValue());
        System.out.println( "Mean time " + data.getMean());
        System.out.println( "50 percentile " + data.getValueAtPercentile(50));
        System.out.println( "75 percentile " + data.getValueAtPercentile(75));
        System.out.println( "90 percentile " + data.getValueAtPercentile(90));
        System.out.println( "95 percentile " + data.getValueAtPercentile(95));
        System.out.println( "99 percentile " + data.getValueAtPercentile(99));
        System.out.println( "99.9 percentile " + data.getValueAtPercentile(99.9));
        System.out.println( "Percentile for less than 1ms " + data.getPercentileAtOrBelowValue(1000));
        System.out.println( "Percentile for less than 99.999ms " + data.getPercentileAtOrBelowValue(99_999));
    }

    private static long getDiffInStats(int i) {
        return Stats.delaysBeforeLatenciesAfter[i];
    }

}
