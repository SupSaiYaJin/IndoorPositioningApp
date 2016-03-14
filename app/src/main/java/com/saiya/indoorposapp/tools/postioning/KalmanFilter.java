package com.saiya.indoorposapp.tools.postioning;

import java.util.List;

/**
 * 卡尔曼滤波器
 */
public class KalmanFilter implements Filter{

    private double estimate;
    private double pdelt;
    private double mdelt;
    private double initialMdelt;
    private final static double Q = 0.00001;
    private final static double R = 0.1;

    /**
     * 以初始值构造卡尔曼滤波器
     * @param pdelt 系统测量误差
     * @param mdelt 最优误差
     */
    public KalmanFilter(double pdelt, double mdelt) {
        /** 系统测量误差 */
        this.pdelt = pdelt;
        /** 最优误差 */
        this.mdelt = mdelt;
        this.initialMdelt = mdelt;
    }

    private double calculate(double current){
        double predict = estimate;
        double gauss = Math.sqrt(pdelt * pdelt + mdelt * mdelt) + Q;
        double kalmanGain = Math.sqrt((gauss * gauss) / (gauss * gauss + pdelt * pdelt)) + R;
        estimate = kalmanGain * (current - predict) + predict;
        mdelt = Math.sqrt((1- kalmanGain) * gauss * gauss);
        return estimate;
    }

    @Override
    public float doFilter(List<Float> values) {
        mdelt = initialMdelt;
        if (values.size() == 0) {
            return 0;
        } else {
            estimate = values.get(0);
            for (int i = 1; i < values.size(); ++i) {
                calculate(values.get(i));
            }
        }
        return (float) estimate;
    }
}
