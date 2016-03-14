package com.saiya.indoorposapp.tools.postioning;

import java.util.List;

/**
 * 取平均滤波器
 */
public class AverageFilter implements Filter{

    @Override
    public float doFilter(List<Float> values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return (float) sum / values.size();
    }

}
