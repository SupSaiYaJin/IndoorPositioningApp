package com.saiya.indoorposapp.tools.postioning;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试滤波器
 */
public class KalmanFilterTest {

    @Test
    public void testDoFilter() throws Exception {
        List<Float> values = new ArrayList<>();
        values.add(46f);
        values.add(80f);
        values.add(46f);
        values.add(47f);
        values.add(43f);
        values.add(45f);
        values.add(42f);
        values.add(18f);
        values.add(49f);
        Filter averageFilter = new AverageFilter();
        Filter kalmanFilter = new KalmanFilter(3, 2);
        System.out.println("average:" + averageFilter.doFilter(values));
        System.out.println("Kalman:" + kalmanFilter.doFilter(values));
    }

}