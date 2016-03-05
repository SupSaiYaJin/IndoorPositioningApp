package com.saiya.indoorposapp.tools;

/**
 * 卡尔曼滤波器
 */
public class KalmanFilter {

    private double estimate;
    private double pdelt;
    private double mdelt;
    private final static double Q = 0.00001;
    private final static double R = 0.1;

    /**
     * 以初始值构造卡尔曼滤波器
     * @param initial 初始值
     */
    KalmanFilter(double initial) {
        estimate = initial;
        /** 系统测量误差 */
        pdelt = 4;
        /** 最优误差 */
        mdelt = 3;
    }

    public double KalmanFilter(double current){
        double predict = estimate;
        double gauss = Math.sqrt(pdelt * pdelt + mdelt * mdelt) + Q;
        double kalmanGain = Math.sqrt((gauss * gauss) / (gauss * gauss + pdelt * pdelt)) + R;
        estimate = kalmanGain * (current - predict) + predict;
        mdelt = Math.sqrt((1- kalmanGain) * gauss * gauss);
        return estimate;
    }
}
